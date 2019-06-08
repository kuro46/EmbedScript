package com.github.kuro46.embedscript

import com.github.kuro46.embedscript.api.EmbedScriptAPI
import com.github.kuro46.embedscript.command.ESCommandHandler
import com.github.kuro46.embedscript.listener.InteractListener
import com.github.kuro46.embedscript.listener.MoveListener
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.EventType
import com.github.kuro46.embedscript.script.JsonLoader
import com.github.kuro46.embedscript.script.ScriptExporter
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptSerializer
import com.github.kuro46.embedscript.script.executor.ScriptProcessor
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays
import java.util.logging.Logger

/**
 * @author shirokuro
 */
class EmbedScript private constructor(val plugin: Plugin) {
    val dataFolder: Path = plugin.dataFolder.toPath()
    val scriptProcessor: ScriptProcessor
    val logger: Logger = plugin.logger
    val scriptManager: ScriptManager
    val configuration: Configuration
    val requests: Requests
    val scriptExporter: ScriptExporter

    init {

        this.scriptManager = loadScripts()

        this.configuration = loadConfiguration()

        this.scriptExporter = ScriptExporter(dataFolder, scriptManager)
        this.requests = Requests(scriptManager)

        this.scriptProcessor = ScriptProcessor(this)

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

        return ScriptManager(JsonLoader(filePath))
    }

    private fun migrateFromOldFormatIfNeeded(scriptFilePath: Path) {
        if (Files.exists(scriptFilePath)) {
            return
        }

        val merged = ScriptManager()
        Arrays.stream(EventType.values())
            .map { eventType -> dataFolder.resolve(eventType.fileName) }
            .filter { path -> Files.exists(path) }
            .map { path -> Pair(path, ScriptManager(JsonLoader(path))) }
            .forEach { (path, scriptManager) ->
                scriptManager.getScripts().forEach { (position, scriptList) ->
                    merged.addAll(position, scriptList)
                }

                Files.delete(path)
            }

        if (merged.getScripts().isEmpty()) {
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
            val executor = ESCommandHandler(this, eventType.presetName)
            val pluginCommand = Bukkit.getPluginCommand(eventType.commandName)
            pluginCommand.executor = executor
            pluginCommand.tabCompleter = executor
        }
        val pluginCommand = Bukkit.getPluginCommand("embedscript")
        val esCommandExecutor = ESCommandHandler(this)
        pluginCommand.executor = esCommandExecutor
        pluginCommand.tabCompleter = esCommandExecutor
    }

    private fun registerESAPI() {
        Bukkit.getServicesManager().register(
            EmbedScriptAPI::class.java,
            EmbedScriptAPI(scriptProcessor),
            plugin,
            ServicePriority.Normal
        )
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
