package com.github.kuro46.embedscript

import com.github.kuro46.embedscript.migrator.ScriptBlockMigrator
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.request.RequestType
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.*
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
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

/**
 * @author shirokuro
 */
class ESCommandExecutor constructor(private val embedScript: EmbedScript, private val presetName: String? = null) : CommandExecutor {
    private val configuration: Configuration = embedScript.configuration
    private val scriptProcessor: ScriptProcessor = embedScript.scriptProcessor
    private val scriptUI: ScriptUI = embedScript.scriptUI
    private val requests: Requests = embedScript.requests
    private val scriptManager: ScriptManager = embedScript.scriptManager

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
            "embed" -> return HandleResult.getByBoolean(modifyAction(player, args, RequestType.EMBED))
            "add" -> return HandleResult.getByBoolean(modifyAction(player, args, RequestType.ADD))
            else -> return HandleResult.UNMATCH
        }
    }

    private fun help(sender: CommandSender) {
        sender.sendMessage(arrayOf("/es help - displays this message", "/es reload - reloads configuration and scripts", "/es migrate - migrates from ScriptBlock", "/es list [world] [page] - displays list of scripts", "/es view - displays information of the script in the clicked block", "/es remove - removes the script in the clicked block", "/es embed <script> - embeds a script to the clicked block", "/es add <script> - adds a script to the clicked block"))
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
            try {
                ScriptBlockMigrator.migrate(embedScript)
            } catch (e: InvalidConfigurationException) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to migrate data of ScriptBlock!")
                System.err.println("Failed to migrate data of ScriptBlock!")
                e.printStackTrace()
                return@execute
            } catch (e: ParseException) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to migrate data of ScriptBlock!")
                System.err.println("Failed to migrate data of ScriptBlock!")
                e.printStackTrace()
                return@execute
            } catch (e: IOException) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to migrate data of ScriptBlock!")
                System.err.println("Failed to migrate data of ScriptBlock!")
                e.printStackTrace()
                return@execute
            }

            sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully migrated!")
        }
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
        scriptUI.list(player, world, filter, pageIndex)
    }

    private fun view(player: Player) {
        player.sendMessage(Prefix.PREFIX + "Click the block to view the script.")
        requests.putRequest(player, Request(RequestType.VIEW))
    }

    private fun remove(player: Player) {
        player.sendMessage(Prefix.PREFIX + "Click the block to remove the script.")
        requests.putRequest(player, Request(RequestType.REMOVE))
    }

    private fun modifyAction(player: Player, args: Array<String>, requestType: RequestType): Boolean {
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
        requests.putRequest(player, Request(requestType, script))

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
