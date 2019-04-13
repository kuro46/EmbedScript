package com.github.kuro46.embedscript.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.command.CommandExecutor as BukkitCommandExecutor

abstract class RootCommandHandler : CommandHandler(), BukkitCommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        super.handleCommandAsRoot(sender, command.name, args.toList())
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return super.handleTabCompleteAsRoot(sender, args.toList())
    }
}
