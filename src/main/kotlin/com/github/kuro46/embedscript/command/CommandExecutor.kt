package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender

interface CommandExecutor {
    fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean
}
