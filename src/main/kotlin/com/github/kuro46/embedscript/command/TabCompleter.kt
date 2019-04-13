package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender

interface TabCompleter {
    fun onTabComplete(sender: CommandSender, uncompletedArg: String, completedArgs: List<String>): List<String>
}
