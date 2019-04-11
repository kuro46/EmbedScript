package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.migrator.ScriptBlockMigrator
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.script.*
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import com.github.kuro46.embedscript.util.Scheduler
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.entity.Player
import java.io.IOException
import java.nio.file.Files

class ESCommandExecutor constructor(embedScript: EmbedScript, private val presetName: String? = null) : RootCommandExecutor() {
    private val configuration = embedScript.configuration
    private val scriptProcessor = embedScript.scriptProcessor
    private val scriptUI = embedScript.scriptUI
    private val requests = embedScript.requests
    private val scriptManager = embedScript.scriptManager
    private val scriptExporter = embedScript.scriptExporter

    init {
        registerChildExecutor("help", HelpExecutor())
        registerChildExecutor("migrate", MigrateExecutor(embedScript))
        registerChildExecutor("export", ExportExecutor(scriptExporter))
        registerChildExecutor("import", ImportExecutor(scriptExporter))
        registerChildExecutor("reload", ReloadExecutor(configuration, scriptManager))
        registerChildExecutor("teleport", TeleportExecutor())
        registerChildExecutor("page", PageExecutor(scriptUI))
        registerChildExecutor("list", ListExecutor(presetName, scriptProcessor, scriptUI))
        registerChildExecutor("view", CommandExecutorUtil.newExecutor(SenderType.Player()) { sender, _, _ ->
            val player = sender as Player
            player.sendMessage(Prefix.PREFIX + "Click the block to view the script.")
            requests.putRequest(player, Request.View)
            true
        })
        registerChildExecutor("remove", CommandExecutorUtil.newExecutor(SenderType.Player()) { sender, _, _ ->
            val player = sender as Player
            player.sendMessage(Prefix.PREFIX + "Click the block to remove the script.")
            requests.putRequest(player, Request.Remove)
            true
        })
        registerChildExecutor("embed", CommandExecutorUtil.newExecutor(SenderType.Player()) { sender, _, args ->
            val player = sender as Player
            modifyAction(player, args, false)
        })
        registerChildExecutor("add", CommandExecutorUtil.newExecutor(SenderType.Player()) { sender, _, args ->
            val player = sender as Player
            modifyAction(player, args, true)
        })
    }

    override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
        return false
    }

    private fun modifyAction(player: Player, args: List<String>, add: Boolean): Boolean {
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

        player.sendMessage(Prefix.PREFIX + "Click the block to add a script.")
        val request = if (add) Request.Add(script) else Request.Embed(script)
        requests.putRequest(player, request)

        return true
    }

    private class HelpExecutor : CommandExecutor() {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            sender.sendMessage("""/es help - displays this message
                    |/es reload - reloads configuration and scripts
                    |/es migrate - migrates from ScriptBlock
                    |/es list [world] [page] - displays list of scripts
                    |/es view - displays information of the script in the clicked block
                    |/es remove - removes the script in the clicked block
                    |/es embed <script> - embeds a script to the clicked block
                    |/es add <script> - adds a script to the clicked block
                    |/es export <world> [fileName] - exports all scripts in the <world> to [fileName] or <world>.json
                    |/es import <fileName> imports all scripts in the specified file""".trimMargin())
            return true
        }
    }

    private class MigrateExecutor(val embedScript: EmbedScript) : CommandExecutor() {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            Scheduler.execute {
                sender.sendMessage("Migrating data of ScriptBlock...")
                val migrationResult = runCatching { ScriptBlockMigrator.migrate(embedScript) }
                migrationResult.exceptionOrNull()?.also {
                    sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to migrate data of ScriptBlock!")
                    System.err.println("Failed to migrate data of ScriptBlock!")
                    it.printStackTrace()
                } ?: run {
                    sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully migrated!")
                }
            }
            return true
        }
    }

    private class ExportExecutor(val scriptExporter: ScriptExporter) : CommandExecutor() {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            if (args.isEmpty()) {
                return false
            }

            Scheduler.execute {
                sender.sendMessage("Exporting...")
                val world = args[0]
                val fileName = ScriptExporter.appendJsonExtensionIfNeeded(args.getOrElse(1) { world })
                val filePath = scriptExporter.resolveByExportFolder(fileName)

                if (Files.exists(filePath)) {
                    sender.sendMessage("File: '$fileName' already exists!")
                    return@execute
                }

                scriptExporter.export(world, filePath)
                sender.sendMessage("All scripts in the '$world' was successfully exported to '$fileName'!")
            }
            return true
        }
    }

    private class ImportExecutor(val scriptExporter: ScriptExporter) : CommandExecutor() {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            if (args.isEmpty()) {
                return false
            }

            Scheduler.execute {
                sender.sendMessage("Importing...")
                val fileName = args[0]
                val filePath = scriptExporter.resolveByExportFolder(fileName)
                if (Files.notExists(filePath)) {
                    sender.sendMessage("File: '$fileName' not exists!")
                    return@execute
                }

                scriptExporter.import(filePath)
                sender.sendMessage("Scripts were successfully imported from '$fileName'!")
            }

            return true
        }
    }

    private class ReloadExecutor(val configuration: Configuration, val scriptManager: ScriptManager) : CommandExecutor() {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            Scheduler.execute {
                sender.sendMessage(Prefix.PREFIX + "Reloading configuration and scripts...")
                try {
                    configuration.load()
                } catch (e: IOException) {
                    sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to reload configuration! (error: " + e.message + ")")
                    e.printStackTrace()
                    return@execute
                } catch (e: InvalidConfigurationException) {
                    sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to reload configuration! (error: " + e.message + ")")
                    e.printStackTrace()
                    return@execute
                }

                try {
                    scriptManager.reload()
                } catch (e: IOException) {
                    sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to reload scripts! (error: " + e.message + ")")
                    e.printStackTrace()
                }

                sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully reloaded!")
            }
            return true
        }
    }

    private class TeleportExecutor : CommandExecutor(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            val player = sender as Player
            if (args.size < 4) {
                return false
            }
            val world = Bukkit.getWorld(args[0])
            if (world == null) {
                player.sendMessage(Prefix.ERROR_PREFIX + "World: " + args[1] + " not exist.")
                return true
            }
            try {
                val playerLocation = player.location
                player.teleport(Location(world,
                        Integer.parseInt(args[1]) + 0.5,
                        Integer.parseInt(args[2]).toDouble(),
                        Integer.parseInt(args[3]) + 0.5,
                        playerLocation.yaw,
                        playerLocation.pitch))
            } catch (e: NumberFormatException) {
                player.sendMessage("X or Y or Z is not valid number.")
                return true
            }

            player.sendMessage(Prefix.SUCCESS_PREFIX + "Teleported.")
            return true
        }
    }

    private class PageExecutor(val scriptUI: ScriptUI) : CommandExecutor(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            val player = sender as Player
            if (args.isEmpty()) {
                return false
            }

            val parsed: Int
            try {
                parsed = Integer.parseInt(args[0])
            } catch (e: NumberFormatException) {
                player.sendMessage(Prefix.ERROR_PREFIX + args[0] + " is not a valid number!")
                return true
            }

            scriptUI.changePage(player, parsed)
            return true
        }
    }

    private class ListExecutor(val presetName: String?,
                               val scriptProcessor: ScriptProcessor,
                               val scriptUI: ScriptUI) : CommandExecutor(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
            val player = sender as Player
            val world = if (args.isEmpty())
                player.world.name
            else
                args[0]
            val pageIndex = if (args.size >= 2 && NumberUtils.isNumber(args[2]))
                Integer.parseInt(args[1]) - 1
            else
                0
            val filter: Script?
            if (presetName == null) {
                filter = null
            } else {
                try {
                    filter = scriptProcessor.parse(player.uniqueId, "@preset " + ScriptUtil.toString(presetName))
                } catch (e: ParseException) {
                    player.sendMessage(Prefix.ERROR_PREFIX + "Failed to filter the scripts. (error: ${e.message})")
                    return true
                }
            }
            val scope = if (world == "all") ScriptUI.ListScope.Server else ScriptUI.ListScope.World(world)
            scriptUI.list(player, scope, filter, pageIndex)
            return true
        }
    }
}
