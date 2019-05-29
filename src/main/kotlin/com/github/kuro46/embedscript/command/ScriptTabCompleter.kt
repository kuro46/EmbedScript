package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.script.executor.ScriptExecutor
import com.github.kuro46.embedscript.util.command.Arguments
import com.github.kuro46.embedscript.util.command.TabCompleter
import org.bukkit.command.CommandSender

/**
 * @author shirokuro
 */
class ScriptTabCompleter(private val scriptExecutor: ScriptExecutor) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        uncompletedArg: String,
        uncompletedArgIndex: Int,
        completedArgs: Arguments
    ): List<String> {
        return if (isKey(completedArgs)) {
            // uncompleted arg is key
            scriptExecutor.getKeys().map { "@$it" }
        } else {
            // uncompleted arg is value

            // TODO: suggest

            listOf("[]")
        }
    }

    /**
     * Checks uncompleted argument is key.
     *
     * @return true if uncompleted argument is key
     */
    private fun isKey(completedArgs: List<String>): Boolean {
        // if completedArgs.size is odd number, uncompletedArg is value.
        // else if completedArgs.size is even number, uncompletedArg is key

        return completedArgs.size % 2 == 0
    }
}
