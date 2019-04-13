package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender

object CommandHandlerUtil {
    fun newHandler(senderType: CommandHandler.SenderType = CommandHandler.SenderType.All,
                   onCommand: (CommandSender, String, List<String>) -> Boolean,
                   onTabComplete: (CommandSender, String, List<String>) -> List<String>): CommandHandler {
        return object : CommandHandler(senderType) {
            override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
                return onCommand(sender, command, args)
            }

            override fun onTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: List<String>): List<String> {
                return onTabComplete(sender, uncompletedArg, completedArgs)
            }
        }
    }

    fun newHandler(senderType: CommandHandler.SenderType = CommandHandler.SenderType.All,
                   onCommand: (CommandSender, String, List<String>) -> Boolean): CommandHandler {
        return newHandler(senderType, onCommand) { _, _, _ -> emptyList() }
    }
}
