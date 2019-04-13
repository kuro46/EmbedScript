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

abstract class CommandExecutor(private val senderType: SenderType = SenderType.All, private val async: Boolean = true) {
    private val childExecutors: ConcurrentMap<String, CommandExecutor> = ConcurrentHashMap()

    fun registerChildExecutor(command: String, executor: CommandExecutor) {
        childExecutors[command.toLowerCase(Locale.ENGLISH)] = executor
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
            childExecutors[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childExecutor ->
                return childExecutor.handleCommand(sender, command, args.stream().skip(1).toList())
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

    protected abstract fun onTabComplete(sender: CommandSender, completedArgs: String, args: List<String>): List<String>

    private fun handleTabComplete(sender: CommandSender, needComplete: String, completedArgs: List<String>): List<String> {
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
            childExecutors[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childExecutor ->
                return childExecutor.handleTabComplete(sender, needComplete, completedArgs.stream().skip(1).toList())
            }
        }

        val suggestions = onTabComplete(sender, needComplete, completedArgs).toMutableList()
        suggestions.addAll(childExecutors.keys)
        return suggestions.filter { it.startsWith(needComplete, true) }
    }

    fun handleTabCompleteAsRoot(sender: CommandSender, args: List<String>): List<String> {
        val (needComplete, completedArgs) = if (args.isEmpty()) {
            Pair("", args)
        } else {
            Pair(args.last(), args.dropLast(1))
        }
        return handleTabComplete(sender, needComplete, completedArgs.stream().filter { it.isNotEmpty() }.toList())
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
