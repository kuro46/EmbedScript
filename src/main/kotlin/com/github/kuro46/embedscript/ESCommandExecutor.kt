package com.github.kuro46.embedscript

import com.github.kuro46.embedscript.migrator.ScriptBlockMigrator
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptUI
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.util.Scheduler
import com.github.kuro46.embedscript.util.Util
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.entity.Player
import java.io.IOException
import java.util.*
import java.util.logging.Level

/**
 * @author shirokuro
 */
class ESCommandExecutor constructor(private val embedScript: EmbedScript, private val presetName: String? = null) : CommandExecutor {
    private val configuration = embedScript.configuration
    private val scriptProcessor = embedScript.scriptProcessor
    private val scriptUI = embedScript.scriptUI
    private val requests = embedScript.requests
    private val scriptManager = embedScript.scriptManager
    private val scriptExporter = embedScript.scriptExporter

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }

        val consoleHandleResult = handleConsoleCommand(sender, args)
        if (consoleHandleResult != HandleResult.UNMATCH) {
            return consoleHandleResult.toBoolean()
        }

        if (sender !is Player) {
            sender.sendMessage("Cannot perform this command from the console.")
            return true
        }

        return handlePlayerCommand(sender, args).toBoolean()
    }

    private fun handleConsoleCommand(sender: CommandSender, args: Array<String>): HandleResult {
        return when (args[0].toLowerCase(Locale.ENGLISH)) {
            "help" -> {
                help(sender)
                HandleResult.COLLECT_USE
            }
            "reload" -> {
                reload(sender)
                HandleResult.COLLECT_USE
            }
            "migrate" -> {
                migrate(sender)
                HandleResult.COLLECT_USE
            }
            "export" -> {
                HandleResult.getByBoolean(export(sender, args))
            }
            "import" -> {
                HandleResult.getByBoolean(import(sender, args))
            }
            else -> HandleResult.UNMATCH
        }
    }

    private fun handlePlayerCommand(player: Player, args: Array<String>): HandleResult {
        when (args[0].toLowerCase(Locale.ENGLISH)) {
            "teleport" -> return HandleResult.getByBoolean(teleport(player, args))
            "page" -> return HandleResult.getByBoolean(page(player, args))
            //Script operations
            "list" -> {
                list(player, args)
                return HandleResult.COLLECT_USE
            }
            "view" -> {
                view(player)
                return HandleResult.COLLECT_USE
            }
            "remove" -> {
                remove(player)
                return HandleResult.COLLECT_USE
            }
            "embed" -> return HandleResult.getByBoolean(modifyAction(player, args, false))
            "add" -> return HandleResult.getByBoolean(modifyAction(player, args, true))
            else -> return HandleResult.UNMATCH
        }
    }

    private fun help(sender: CommandSender) {
        sender.sendMessage(arrayOf("/es help - displays this message",
                "/es reload - reloads configuration and scripts",
                "/es migrate - migrates from ScriptBlock",
                "/es list [world] [page] - displays list of scripts",
                "/es view - displays information of the script in the clicked block",
                "/es remove - removes the script in the clicked block",
                "/es embed <script> - embeds a script to the clicked block",
                "/es add <script> - adds a script to the clicked block",
                "/es export <world> <fileName> - exports all scripts in the <world> to <fileName>",
                "/es import <fileName> imports all scripts in the specified file"))
    }

    private fun reload(sender: CommandSender) {
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
    }

    private fun migrate(sender: CommandSender) {
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
    }

    private fun export(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size <= 2) {
            return false
        }

        Scheduler.execute {
            sender.sendMessage("Exporting...")
            val world = args[1]
            val result = runCatching { scriptExporter.export(world, args[2]) }

            if (result.isSuccess) {
                sender.sendMessage("All scripts in the '$world' " +
                        "was successfully exported to '${result.getOrNull()!!}'!")
            } else {
                val message = "Failed to export the scripts in the '$world'!"
                embedScript.logger.log(Level.SEVERE, message, result.exceptionOrNull()!!)
                sender.sendMessage("$message Please see the console log")
            }
        }
        return true
    }

    private fun import(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size <= 1) {
            return false
        }

        Scheduler.execute {
            sender.sendMessage("Importing...")
            val fileName = args[1]
            val result = runCatching { scriptExporter.import(fileName) }

            if (result.isSuccess) {
                sender.sendMessage("Scripts were successfully imported from '$fileName'!")
            } else {
                val message = "Failed to import the scripts from '$fileName'!"
                embedScript.logger.log(Level.SEVERE, message, result.exceptionOrNull()!!)
                sender.sendMessage("$message Please see the console log")
            }
        }

        return true
    }

    private fun teleport(player: Player, args: Array<String>): Boolean {
        if (args.size < 5) {
            return false
        }
        val world = Bukkit.getWorld(args[1])
        if (world == null) {
            player.sendMessage(Prefix.ERROR_PREFIX + "World: " + args[1] + " not exist.")
            return true
        }
        try {
            val playerLocation = player.location
            player.teleport(Location(world,
                Integer.parseInt(args[2]) + 0.5,
                Integer.parseInt(args[3]).toDouble(),
                Integer.parseInt(args[4]) + 0.5,
                playerLocation.yaw,
                playerLocation.pitch))
        } catch (e: NumberFormatException) {
            player.sendMessage("X or Y or Z is not valid number.")
            return true
        }

        player.sendMessage(Prefix.SUCCESS_PREFIX + "Teleported.")
        return true
    }

    private fun page(player: Player, args: Array<String>): Boolean {
        if (args.size < 2) {
            return false
        }

        val parsed: Int
        try {
            parsed = Integer.parseInt(args[1])
        } catch (e: NumberFormatException) {
            player.sendMessage(Prefix.ERROR_PREFIX + args[1] + " is not a valid number!")
            return true
        }

        scriptUI.changePage(player, parsed)
        return true
    }

    private fun list(player: Player, args: Array<String>) {
        val world = if (args.size < 2)
            player.world.name
        else
            args[1]
        val pageIndex = if (args.size >= 3 && NumberUtils.isNumber(args[2]))
            Integer.parseInt(args[2]) - 1
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
                return
            }
        }
        val scope = if (world == "all") ScriptUI.ListScope.Server else ScriptUI.ListScope.World(world)
        scriptUI.list(player, scope, filter, pageIndex)
    }

    private fun view(player: Player) {
        player.sendMessage(Prefix.PREFIX + "Click the block to view the script.")
        requests.putRequest(player, Request.View)
    }

    private fun remove(player: Player) {
        player.sendMessage(Prefix.PREFIX + "Click the block to remove the script.")
        requests.putRequest(player, Request.Remove)
    }

    private fun modifyAction(player: Player, args: Array<String>, add: Boolean): Boolean {
        if (args.size < 2) {
            return false
        }
        val stringScript = Util.joinStringSpaceDelimiter(1, args)
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

    private enum class HandleResult {
        COLLECT_USE,
        INCORRECT_USE,
        UNMATCH;

        fun toBoolean(): Boolean {
            return when (this) {
                UNMATCH, INCORRECT_USE -> false
                COLLECT_USE -> true
            }
        }

        companion object {

            fun getByBoolean(bool: Boolean): HandleResult {
                return if (bool) COLLECT_USE else INCORRECT_USE
            }
        }
    }
}
