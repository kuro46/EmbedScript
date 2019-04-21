package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import org.bukkit.command.CommandSender

class ScriptTabCompleter(private val scriptProcessor: ScriptProcessor) : TabCompleter {
    override fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
        return if (isKey(completedArgs)) {
            scriptProcessor.getProcessors().keys.map { "@$it" }
        } else {
            emptyList()
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
