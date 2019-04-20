package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.migrator.ScriptBlockMigrator
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptExporter
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptUI
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.nio.file.Files
import kotlin.streams.toList

class ESCommandHandler constructor(embedScript: EmbedScript, private val presetName: String? = null) : RootCommandHandler() {
    private val configuration = embedScript.configuration
    private val scriptProcessor = embedScript.scriptProcessor
    private val scriptUI = embedScript.scriptUI
    private val requests = embedScript.requests
    private val scriptManager = embedScript.scriptManager
    private val scriptExporter = embedScript.scriptExporter

    init {
        registerChildHandler("help", HelpHandler())
        registerChildHandler("migrate", MigrateHandler(embedScript))
        registerChildHandler("export", ExportHandler(scriptExporter))
        registerChildHandler("import", ImportHandler(scriptExporter))
        registerChildHandler("reload", ReloadHandler(configuration, scriptManager))
        registerChildHandler("teleport", TeleportHandler())
        registerChildHandler("page", PageHandler(scriptUI))
        registerChildHandler("list", ListHandler(presetName, scriptProcessor, scriptUI))
        registerChildHandler("listAll", ListAllHandler(presetName, scriptProcessor, scriptUI))
        registerChildHandler("view", CommandHandlerUtil.newHandler(SenderType.Player()) { sender, _, _ ->
            val player = sender as Player
            player.sendMessage(Prefix.PREFIX + "Please click any block...")
            requests.putRequest(player, Request.View)
            true
        })
        registerChildHandler("remove", CommandHandlerUtil.newHandler(SenderType.Player()) { sender, _, _ ->
            val player = sender as Player
            player.sendMessage(Prefix.PREFIX + "Please click any block...")
            requests.putRequest(player, Request.Remove)
            true
        })
        val scriptTabCompleter = ScriptTabCompleter(scriptProcessor)
        registerChildHandler("embed", CommandHandlerUtil.newHandler(SenderType.Player()) { sender, _, args ->
            val player = sender as Player
            modifyAction(player, args, false)
        }.apply { this.tabCompleter = scriptTabCompleter })
        registerChildHandler("add", CommandHandlerUtil.newHandler(SenderType.Player()) { sender, _, args ->
            val player = sender as Player
            modifyAction(player, args, true)
        }.apply { this.tabCompleter = scriptTabCompleter })
    }

    override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        return false
    }

    private fun modifyAction(player: Player, args: Arguments, add: Boolean): Boolean {
        if (args.isEmpty()) {
            return false
        }
        val stringScript = args.joinToString(" ")
        val script: Script
        try {
            val preset = if (presetName == null)
                ""
            else
                "@preset " + ScriptUtil.toString(presetName) + " "
            script = scriptProcessor.parse(player.uniqueId, preset + stringScript)
        } catch (e: ParseException) {
            player.sendMessage(Prefix.ERROR_PREFIX + "Failed to filter the scripts. (error: ${e.message})")
            return true
        }

        player.sendMessage(Prefix.PREFIX + "Please click any block...")
        val request = if (add) Request.Add(script) else Request.Embed(script)
        requests.putRequest(player, request)

        return true
    }

    private class HelpHandler : CommandHandler() {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            sender.sendMessage("""/es help - Displays this message.
                    |/es reload - Reloads configuration and scripts
                    |/es migrate - Migrates from ScriptBlock to this plugin.
                    |/es list [world] [page] - Displays list of scripts in the [world] or current world.
                    |/es listAll [page] - Displays list of scripts in this server.
                    |/es view - Displays information about script in the clicked block.
                    |/es remove - Removes all scripts in the clicked block.
                    |/es embed <script> - Embeds a script to the clicked block.
                    |/es add <script> - Adds a script to the clicked block
                    |/es export <world> [fileName] - Exports all scripts in the <world> to [fileName] or <world>.
                    |/es import <fileName> Imports all scripts in the <fileName>.""".trimMargin())
            return true
        }
    }

    private class MigrateHandler(val embedScript: EmbedScript) : CommandHandler() {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            sender.sendMessage("Migrating data of ScriptBlock...")
            val migrationResult = runCatching { ScriptBlockMigrator.migrate(embedScript) }
            migrationResult.exceptionOrNull()?.also {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Migration failed!")
                System.err.println("Migration failed!")
                it.printStackTrace()
            } ?: run {
                sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully migrated!")
            }
            return true
        }
    }

    private class ExportHandler(val scriptExporter: ScriptExporter) : CommandHandler() {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            if (args.isElementNotEnough(0)) {
                return false
            }

            sender.sendMessage("Exporting...")
            val world = args[0]
            val fileName = ScriptExporter.appendJsonExtensionIfNeeded(args.getOrElse(1) { world })
            val filePath = scriptExporter.resolveByExportFolder(fileName)

            if (Files.exists(filePath)) {
                sender.sendMessage("File: '$fileName' already exists!")
            } else {
                scriptExporter.export(world, filePath)
                sender.sendMessage("All scripts in the '$world' was successfully exported to '$fileName'!")
            }
            return true
        }

        override fun onTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: Arguments): List<String> {
            return if (completedArgs.isEmpty()) {
                // player wants world list
                Bukkit.getWorlds().stream()
                        .map { it.name }
                        .toList()
            } else {
                emptyList()
            }
        }
    }

    private class ImportHandler(val scriptExporter: ScriptExporter) : CommandHandler() {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            if (args.isElementNotEnough(0)) {
                return false
            }

            sender.sendMessage("Importing...")
            val fileName = args[0]
            val filePath = scriptExporter.resolveByExportFolder(fileName)
            if (Files.notExists(filePath)) {
                sender.sendMessage("File: '$fileName' not exists!")
            } else {
                scriptExporter.import(filePath)
                sender.sendMessage("Scripts were successfully imported from '$fileName'!")
            }

            return true
        }

        override fun onTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: Arguments): List<String> {
            // TODO: Returns list of files in the EmbedScript/export
            return emptyList()
        }
    }

    private class ReloadHandler(val configuration: Configuration, val scriptManager: ScriptManager) : CommandHandler() {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            sender.sendMessage(Prefix.PREFIX + "Reloading configuration and scripts...")

            val cfgReloadResult = runCatching { configuration.load() }
            cfgReloadResult.exceptionOrNull()?.let {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Reload failed! (error: " + it.message + ")")
                it.printStackTrace()
                return true
            }

            val scriptReloadResult = runCatching { scriptManager.reload() }
            scriptReloadResult.exceptionOrNull()?.let {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Reload failed! (error: " + it.message + ")")
                it.printStackTrace()
                return true
            }

            sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully reloaded!")
            return true
        }
    }

    private class TeleportHandler : CommandHandler(SenderType.Player(), false) {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            val player = sender as Player
            if (args.isElementNotEnough(3)) {
                return false
            }
            val world = args.getOrNull(0)?.let {
                Bukkit.getWorld(it)
            } ?: run {
                player.sendMessage(Prefix.ERROR_PREFIX + "World: " + args[0] + " not exist.")
                return true
            }

            val playerLocation = player.location
            val x = args.getInt(sender, 1) ?: return true
            val y = args.getInt(sender, 2) ?: return true
            val z = args.getInt(sender, 3) ?: return true
            player.teleport(Location(world,
                    x + 0.5,
                    y.toDouble(),
                    z + 0.5,
                    playerLocation.yaw,
                    playerLocation.pitch))

            player.sendMessage(Prefix.SUCCESS_PREFIX + "Teleported.")
            return true
        }
    }

    private class PageHandler(val scriptUI: ScriptUI) : CommandHandler(SenderType.Player(), false) {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            val player = sender as Player
            if (args.isElementNotEnough(0)) {
                return false
            }

            val pageIndex = args.getInt(sender, 0) ?: return true
            scriptUI.changePage(player, pageIndex)
            return true
        }
    }

    private class ListHandler(val presetName: String?,
                              val scriptProcessor: ScriptProcessor,
                              val scriptUI: ScriptUI) : CommandHandler(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            val player = sender as Player
            val world = args.getOrElse(0) { player.world.name }
            val pageIndex = args.getInt(sender, 1)?.minus(1) ?: return true
            val filter = presetName?.let {
                scriptProcessor.parse(player.uniqueId, "@preset " + ScriptUtil.toString(it))
            }
            val scope = ScriptUI.ListScope.World(world)
            scriptUI.list(player, scope, filter, pageIndex)
            return true
        }

        override fun onTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: Arguments): List<String> {
            return if (completedArgs.isEmpty()) {
                // player wants world list
                Bukkit.getWorlds().stream()
                        .map { it.name }
                        .toList()
            } else {
                emptyList()
            }
        }
    }

    private class ListAllHandler(val presetName: String?,
                                 val scriptProcessor: ScriptProcessor,
                                 val scriptUI: ScriptUI) : CommandHandler(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            val player = sender as Player
            val pageIndex = args.getInt(sender, 0)?.minus(1) ?: return true
            val filter = presetName?.let {
                scriptProcessor.parse(player.uniqueId, "@preset " + ScriptUtil.toString(it))
            }
            val scope = ScriptUI.ListScope.Server
            scriptUI.list(player, scope, filter, pageIndex)
            return true
        }
    }
}
