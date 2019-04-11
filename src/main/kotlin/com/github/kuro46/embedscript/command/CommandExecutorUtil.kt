package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender

object CommandExecutorUtil {
    fun newExecutor(senderType: CommandExecutor.SenderType = CommandExecutor.SenderType.All,
                    handler: (CommandSender, String, List<String>) -> Boolean): CommandExecutor {
        return object : CommandExecutor(senderType) {
            override fun onCommand(sender: CommandSender, command: String, args: List<String>): Boolean {
                return handler(sender, command, args)
            }
        }
    }
}
