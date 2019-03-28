package com.github.kuro46.embedscript

import com.github.kuro46.embedscript.api.EmbedScriptAPI
import com.github.kuro46.embedscript.listener.InteractListener
import com.github.kuro46.embedscript.listener.MoveListener
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.*
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

class EmbedScript private constructor(val plugin: Plugin) {
    val dataFolder: Path = plugin.dataFolder.toPath()
    val logger: Logger = plugin.logger
    val scriptManager: ScriptManager
    val configuration: Configuration
    val scriptUI: ScriptUI
    val requests: Requests
    val scriptProcessor: ScriptProcessor

    init {

        this.scriptManager = loadScripts()

        this.configuration = loadConfiguration()

        this.scriptUI = ScriptUI(scriptManager)
        this.requests = Requests(scriptUI)
        this.scriptProcessor = ScriptProcessor(logger, plugin, configuration)

        registerCommands()
        registerListeners()
        registerESAPI()
    }

    private fun loadConfiguration(): Configuration {
        plugin.saveDefaultConfig()
        return Configuration.load(dataFolder)
    }

    private fun loadScripts(): ScriptManager {
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder)
        }

        val filePath = dataFolder.resolve("scripts.json")

        migrateFromOldFormatIfNeeded(filePath)

        return ScriptManager.load(filePath)
    }

    private fun migrateFromOldFormatIfNeeded(scriptFilePath: Path) {
        if (Files.exists(scriptFilePath)) {
            return
        }

        val merged = HashMap<ScriptPosition, MutableList<Script>>()
        try {
            Arrays.stream(EventType.values())
                .map { eventType -> dataFolder.resolve(eventType.fileName) }
                .filter { path -> Files.exists(path) }
                .map { path ->
                    return@map ScriptManager.load(path)
                }
                .forEach { scriptManager ->
                    for (entry in scriptManager.entrySet()) {
                        val position = entry.key
                        val scripts = entry.value
                        val mergeTo = merged.computeIfAbsent(position) { ArrayList() }

                        mergeTo.addAll(scripts)
                    }

                    try {
                        Files.delete(scriptManager.path)
                    } catch (e: IOException) {
                        throw UncheckedIOException(e)
                    }
                }
        } catch (e: UncheckedIOException) {
            throw e.cause!!
        }

        if (merged.isEmpty()) {
            return
        }

        ScriptSerializer.serialize(scriptFilePath, merged)
    }

    private fun registerListeners() {
        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(InteractListener(this), plugin)
        pluginManager.registerEvents(MoveListener(this), plugin)
    }

    private fun registerCommands() {
        for (eventType in EventType.values()) {
            Bukkit.getPluginCommand(eventType.commandName).executor = ESCommandExecutor(this, eventType.presetName)
        }
        Bukkit.getPluginCommand("embedscript").executor = ESCommandExecutor(this)
    }

    private fun registerESAPI() {
        Bukkit.getServicesManager().register(EmbedScriptAPI::class.java,
            EmbedScriptAPI(scriptProcessor),
            plugin,
            ServicePriority.Normal)
    }

    companion object {
        @Volatile
        private var instance: EmbedScript? = null

        @Synchronized
        fun initialize(plugin: Plugin) {

            if (instance != null) {
                throw IllegalStateException("Instance already initialized!")
            }

            instance = EmbedScript(plugin)
        }

        @Synchronized
        fun reset() {
            instance = null
        }
    }
}
