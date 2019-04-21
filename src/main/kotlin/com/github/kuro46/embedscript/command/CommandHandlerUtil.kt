package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

object CommandHandlerUtil {
    fun newHandler(senderType: CommandHandler.SenderType = CommandHandler.SenderType.All,
                   onCommand: (CommandSender, String, Arguments) -> Boolean,
                   onTabComplete: (CommandSender, String, Int, Arguments) -> List<String>): CommandHandler {
        return object : CommandHandler(senderType) {
            override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
                return onCommand(sender, command, args)
            }

            override fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
                return onTabComplete(sender, uncompletedArg, uncompletedArgIndex, completedArgs)
            }
        }
    }

    fun newHandler(senderType: CommandHandler.SenderType = CommandHandler.SenderType.All,
                   onCommand: (CommandSender, String, Arguments) -> Boolean): CommandHandler {
        return newHandler(senderType, onCommand) { _, _, _, _ -> emptyList() }
    }

    fun castToPlayer(sender: CommandSender, castFailed: String =
                                           "Cannot perform this command from the console."): Player? {
        return if (sender !is Player) {
            sender.sendMessage(castFailed)
            null
        } else { sender }
    }

    fun castToConsole(sender: CommandSender, castFailed: String =
                                            "Cannot perform this command from the game."): ConsoleCommandSender? {
        return if(sender !is ConsoleCommandSender) {
            sender.sendMessage(castFailed)
            null
        } else { sender }
    }
}
