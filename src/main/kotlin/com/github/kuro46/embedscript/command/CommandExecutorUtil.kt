package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender

object CommandExecutorUtil {
    fun newExecutor(senderType: CommandExecutor.SenderType = CommandExecutor.SenderType.All,
                    onCommand: (CommandSender, String, List<String>) -> Boolean,
                    onTabComplete: (CommandSender, String, List<String>) -> List<String>): CommandExecutor {
        return object : CommandExecutor(senderType) {
            override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
                return onCommand(sender, command, args)
            }

            override fun onTabComplete(sender: CommandSender, completedArgs: String, args: List<String>): List<String> {
                return onTabComplete(sender, completedArgs, args)
            }
        }
    }

    fun newExecutor(senderType: CommandExecutor.SenderType = CommandExecutor.SenderType.All,
                    onCommand: (CommandSender, String, List<String>) -> Boolean): CommandExecutor {
        return newExecutor(senderType, onCommand) { _, _, _ -> emptyList() }
    }
}
