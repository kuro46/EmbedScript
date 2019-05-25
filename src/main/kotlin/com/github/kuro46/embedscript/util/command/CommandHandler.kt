package com.github.kuro46.embedscript.util.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.util.Scheduler
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Future
import kotlin.streams.toList
import org.bukkit.command.CommandExecutor as BukkitCommandExecutor

/**
 * @author shirokuro
 */
abstract class CommandHandler(
        private val senderType: SenderType = SenderType.All,
        private val handlingMode: HandlingMode = HandlingMode.Asynchronous
) : CommandExecutor, TabCompleter {
    private val childHandlers: ConcurrentMap<String, CommandHandler> = ConcurrentHashMap()
    var commandExecutor: CommandExecutor? = null
    var tabCompleter: TabCompleter? = null

    fun registerChildHandler(command: String, handler: CommandHandler) {
        childHandlers[command.toLowerCase(Locale.ENGLISH)] = handler
    }

    private fun checkSenderType(sender: CommandSender): Boolean {
        when (senderType) {
            is SenderType.Console -> {
                senderType.errorMessage?.let {
                    CommandHandlerUtil.castToConsole(sender, it) ?: return false
                } ?: run {
                    CommandHandlerUtil.castToConsole(sender) ?: return false
                }
            }
            is SenderType.Player -> {
                senderType.errorMessage?.let {
                    CommandHandlerUtil.castToPlayer(sender, it) ?: return false
                } ?: run {
                    CommandHandlerUtil.castToPlayer(sender) ?: return false
                }
            }
        }

        return true
    }

    // ----------------
    // Command Handling
    // ----------------

    abstract override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean

    private fun handleCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        if (!checkSenderType(sender)) return true

        // find child executors and execute if contains
        args.getOrNull(0)?.let { firstArg ->
            childHandlers[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childHandler ->
                return childHandler.handleCommand(sender, command, Arguments(args.stream().skip(1).toList()))
            }
        }

        val commandExecutor = this.commandExecutor ?: this

        return if (handlingMode is HandlingMode.Synchronous) {
            Bukkit.getScheduler().callSyncMethod(handlingMode.plugin) {
                commandExecutor.onCommand(sender, command, args)
            }.get()
        } else {
            commandExecutor.onCommand(sender, command, args)
        }
    }

    fun handleCommandAsRoot(sender: CommandSender, command: String, args: Arguments): Future<*> {
        return Scheduler.submit {
            if (!handleCommand(sender, command, args)) {
                sender.sendMessage(Prefix.ERROR + "Incorrect usage!")
            }
        }
    }

    // -----------------------
    // Tab Completion Handling
    // -----------------------

    override fun onTabComplete(
            sender: CommandSender,
            uncompletedArg: String,
            uncompletedArgIndex: Int,
            completedArgs: Arguments
    ): List<String> {
        return emptyList()
    }

    private fun handleTabComplete(
            sender: CommandSender,
            uncompletedArg: String,
            uncompletedArgIndex: Int,
            completedArgs: Arguments
    ): List<String> {
        if (!checkSenderType(sender)) return emptyList()

        // find child executors and execute if contains
        completedArgs.getOrNull(0)?.let { firstArg ->
            childHandlers[firstArg.toLowerCase(Locale.ENGLISH)]?.let { childHandler ->
                return childHandler.handleTabComplete(
                        sender, uncompletedArg,
                        uncompletedArgIndex - 1,
                        Arguments(completedArgs.stream().skip(1).toList())
                )
            }
        }

        val tabCompleter = this.tabCompleter ?: this

        val suggestions = tabCompleter.onTabComplete(
                sender,
                uncompletedArg,
                uncompletedArgIndex,
                completedArgs
        ).toMutableList()
        suggestions.addAll(childHandlers.keys)
        return suggestions.filter { it.startsWith(uncompletedArg, true) }
    }

    fun handleTabCompleteAsRoot(sender: CommandSender, args: List<String>): List<String> {
        val (uncompletedArg, completedArgs) = if (args.isEmpty()) {
            Pair("", args)
        } else {
            Pair(args.last(), args.dropLast(1))
        }

        val withoutEmptyString = completedArgs.filter { it.isNotEmpty() }

        return handleTabComplete(
                sender,
                uncompletedArg,
                args.lastIndex,
                Arguments(withoutEmptyString)
        )
    }

    sealed class SenderType {
        object All : SenderType()
        data class Console(val errorMessage: String? = null) : SenderType()
        data class Player(val errorMessage: String? = null) : SenderType()
    }

    sealed class HandlingMode {
        object Asynchronous : HandlingMode()
        data class Synchronous(val plugin: Plugin) : HandlingMode()
    }
}
