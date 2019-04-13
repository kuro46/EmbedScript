package com.github.kuro46.embedscript.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.streams.toList
import org.bukkit.command.CommandExecutor as BukkitCommandExecutor

abstract class CommandHandler(private val senderType: SenderType = SenderType.All, private val async: Boolean = true) {
    private val childHandlers: ConcurrentMap<String, CommandHandler> = ConcurrentHashMap()

    fun registerChildHandler(command: String, handler: CommandHandler) {
        childHandlers[command.toLowerCase(Locale.ENGLISH)] = handler
    }

    // ----------------
    // Command Handling
    // ----------------

    protected abstract fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean

    private fun handleCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
        when (senderType) {
            is SenderType.Console -> {
                if (sender !is ConsoleCommandSender) {
                    sender.sendMessage(senderType.errorMessage)
                    return true
                }
            }
            is SenderType.Player -> {
                if (sender !is Player) {
                    sender.sendMessage(senderType.errorMessage)
                    return true
                }
            }
        }

        // find child executors and execute if contains
        args.getOrNull(0)?.let { firstArg ->
            childHandlers[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childHandler ->
                return childHandler.handleCommand(sender, command, args.stream().skip(1).toList())
            }
        }

        return if (async) {
            onCommand(sender, command, args)
        } else {
            Bukkit.getScheduler().callSyncMethod(Bukkit.getPluginManager().getPlugin("EmbedScript")) {
                onCommand(sender, command, args)
            }.get()
        }
    }

    fun handleCommandAsRoot(sender: CommandSender, command: String, args: List<String>): Future<*> {
        return commandHandleThread.submit {
            if (!handleCommand(sender, command, args)) {
                sender.sendMessage("Incorrect usage!")
            }
        }
    }

    // -----------------------
    // Tab Completion Handling
    // -----------------------

    protected abstract fun onTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: List<String>): List<String>

    private fun handleTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: List<String>): List<String> {
        when (senderType) {
            is SenderType.Console -> {
                if (sender !is ConsoleCommandSender) {
                    sender.sendMessage(senderType.errorMessage)
                    return emptyList()
                }
            }
            is SenderType.Player -> {
                if (sender !is Player) {
                    sender.sendMessage(senderType.errorMessage)
                    return emptyList()
                }
            }
        }

        // find child executors and execute if contains
        completedArgs.getOrNull(0)?.let { firstArg ->
            childHandlers[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childHandler ->
                return childHandler.handleTabComplete(sender, uncompletedArg, completedArgs.stream().skip(1).toList())
            }
        }

        val suggestions = onTabComplete(sender, uncompletedArg, completedArgs).toMutableList()
        suggestions.addAll(childHandlers.keys)
        return suggestions.filter { it.startsWith(uncompletedArg, true) }
    }

    fun handleTabCompleteAsRoot(sender: CommandSender, args: List<String>): List<String> {
        val (uncompletedArg, completedArgs) = if (args.isEmpty()) {
            Pair("", args)
        } else {
            Pair(args.last(), args.dropLast(1))
        }
        return handleTabComplete(sender, uncompletedArg, completedArgs.stream().filter { it.isNotEmpty() }.toList())
    }

    companion object {
        private val commandHandleThread = Executors.newCachedThreadPool { r ->
            val thread = Thread(r, "EmbedScript-Command-Handling-Thread")
            thread.isDaemon = true
            thread
        }
    }

    sealed class SenderType {
        object All : SenderType()
        data class Console(val errorMessage: String = "Cannot perform this command from the game.") : SenderType()
        data class Player(val errorMessage: String = "Cannot perform this command from the console.") : SenderType()
    }
}
