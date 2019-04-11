package com.github.kuro46.embedscript.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.CommandExecutor as BukkitCommandExecutor

abstract class RootCommandExecutor : CommandExecutor(), BukkitCommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        return super.handleCommand(sender, command.name, args.toList())
    }
}
