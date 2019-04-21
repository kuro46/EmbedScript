package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.util.Scheduler
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Future
import kotlin.streams.toList
import org.bukkit.command.CommandExecutor as BukkitCommandExecutor

abstract class CommandHandler(private val senderType: SenderType = SenderType.All, private val async: Boolean = true) : CommandExecutor, TabCompleter {
    private val childHandlers: ConcurrentMap<String, CommandHandler> = ConcurrentHashMap()
    var commandExecutor: CommandExecutor? = null
    var tabCompleter: TabCompleter? = null

    fun registerChildHandler(command: String, handler: CommandHandler) {
        childHandlers[command.toLowerCase(Locale.ENGLISH)] = handler
    }

    // ----------------
    // Command Handling
    // ----------------

    abstract override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean

    private fun handleCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
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
                return childHandler.handleCommand(sender, command, Arguments(args.stream().skip(1).toList()))
            }
        }

        val commandExecutor = this.commandExecutor ?: this

        return if (async) {
            commandExecutor.onCommand(sender, command, args)
        } else {
            Bukkit.getScheduler().callSyncMethod(Bukkit.getPluginManager().getPlugin("EmbedScript")) {
                commandExecutor.onCommand(sender, command, args)
            }.get()
        }
    }

    fun handleCommandAsRoot(sender: CommandSender, command: String, args: Arguments): Future<*> {
        return Scheduler.submit {
            if (!handleCommand(sender, command, args)) {
                sender.sendMessage("Incorrect usage!")
            }
        }
    }

    // -----------------------
    // Tab Completion Handling
    // -----------------------

    override fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
        return emptyList()
    }

    private fun handleTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
        when (senderType) {
            is SenderType.Console -> {
                senderType.errorMessage?.let {
                    CommandHandlerUtil.castToConsole(sender, it) ?: return emptyList()
                } ?: run {
                    CommandHandlerUtil.castToConsole(sender) ?: return emptyList()
                }
            }
            is SenderType.Player -> {
                senderType.errorMessage?.let {
                    CommandHandlerUtil.castToConsole(sender, it) ?: return emptyList()
                } ?: run {
                    CommandHandlerUtil.castToConsole(sender) ?: return emptyList()
                }
            }
        }

        // find child executors and execute if contains
        completedArgs.getOrNull(0)?.let { firstArg ->
            childHandlers[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childHandler ->
                return childHandler.handleTabComplete(sender, uncompletedArg,
                        uncompletedArgIndex - 1,
                        Arguments(completedArgs.stream().skip(1).toList()))
            }
        }

        val tabCompleter = this.tabCompleter ?: this

        val suggestions = tabCompleter.onTabComplete(sender, uncompletedArg, uncompletedArgIndex, completedArgs).toMutableList()
        suggestions.addAll(childHandlers.keys)
        return suggestions.filter { it.startsWith(uncompletedArg, true) }
    }

    fun handleTabCompleteAsRoot(sender: CommandSender, args: List<String>): List<String> {
        val (uncompletedArg, completedArgs) = if (args.isEmpty()) {
            Pair("", args)
        } else {
            Pair(args.last(), args.dropLast(1))
        }
        return handleTabComplete(sender, uncompletedArg, args.lastIndex, Arguments(completedArgs.stream().filter { it.isNotEmpty() }.toList()))
    }

    sealed class SenderType {
        object All : SenderType()
        data class Console(val errorMessage: String? = null) : SenderType()
        data class Player(val errorMessage: String? = null) : SenderType()
    }
}
