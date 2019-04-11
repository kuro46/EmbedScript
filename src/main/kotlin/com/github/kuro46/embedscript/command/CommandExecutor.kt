package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.command.CommandExecutor as BukkitCommandExecutor
import java.util.*
import kotlin.streams.toList

abstract class CommandExecutor(private val senderType: SenderType = SenderType.All) {
    private val childExecutors: MutableMap<String, CommandExecutor> = HashMap()

    protected abstract fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean

    fun handleCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
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

        return onCommand(sender, command, args)
    }

    fun registerChildExecutor(command: String, executor: CommandExecutor) {
        childExecutors[command.toLowerCase(Locale.ENGLISH)] = executor
    }

    sealed class SenderType {
        object All : SenderType()
        data class Console(val errorMessage: String = "Cannot perform this command from the game.") : SenderType()
        data class Player(val errorMessage: String = "Cannot perform this command from the console.") : SenderType()
    }
}
