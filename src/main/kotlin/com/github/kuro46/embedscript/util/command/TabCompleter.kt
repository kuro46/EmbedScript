package com.github.kuro46.embedscript.util.command

import org.bukkit.command.CommandSender

interface TabCompleter {
    fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String>
}
