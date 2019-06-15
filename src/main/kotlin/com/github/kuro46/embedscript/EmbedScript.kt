package com.github.kuro46.embedscript

import com.github.kuro46.embedscript.api.EmbedScriptAPI
import com.github.kuro46.embedscript.command.AliasCommandHandler
import com.github.kuro46.embedscript.command.ESCommandHandler
import com.github.kuro46.embedscript.util.command.CommandHandlerManager
import com.github.kuro46.embedscript.util.Scheduler
import com.github.kuro46.embedscript.listener.InteractListener
import com.github.kuro46.embedscript.listener.MoveListener
import com.github.kuro46.embedscript.permission.PermissionDetector
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
import java.util.concurrent.Executor

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
    val permissionDetector: PermissionDetector

    init {
        this.permissionDetector = PermissionDetector(dataFolder)

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
        val commandHandlerManager = CommandHandlerManager(
            object : Executor {
                override fun execute(runnable: Runnable) {
                    Scheduler.execute() { runnable.run() }
                }
            },
            plugin
        )
        for (eventType in EventType.values()) {
            AliasCommandHandler.registerHandlers(
                commandHandlerManager,
                eventType,
                scriptProcessor,
                requests
            )
        }
        ESCommandHandler(this, commandHandlerManager)
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
