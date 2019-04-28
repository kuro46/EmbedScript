package com.github.kuro46.embedscript.util.command

import org.bukkit.command.CommandSender

/**
 * @author shirokuro
 */
interface CommandExecutor {
    fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean
}
