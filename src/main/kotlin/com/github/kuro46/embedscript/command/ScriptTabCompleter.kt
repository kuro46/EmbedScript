package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import com.github.kuro46.embedscript.util.command.Arguments
import com.github.kuro46.embedscript.util.command.TabCompleter
import org.bukkit.command.CommandSender

class ScriptTabCompleter(private val scriptProcessor: ScriptProcessor) : TabCompleter {
    override fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
        return if (isKey(completedArgs)) {
            // uncompleted arg is key
            scriptProcessor.getProcessors().keys.map { "@$it" }
        } else {
            // uncompleted arg is value

            val key = completedArgs.last()
            // remove '@'
            val processorName = scriptProcessor.scriptParser.unOmitValue(removeFirstChar(key))
                    ?: run {
                        sender.sendMessage("'$key' is unknown key!")
                        return emptyList()
                    }
            val suggestions =
                    scriptProcessor.getProcessors().getValue(processorName).parser.getSuggestions(uncompletedArg)

            return if (suggestions.isNotEmpty()) {
                val surrounded: MutableList<String> = ArrayList(suggestions.size + 1)
                suggestions.forEach { surrounded.add("[$it]") }
                surrounded.add("[]")

                surrounded
            } else {
                listOf("[]")
            }
        }
    }

    private fun removeFirstChar(str: String): String {
        return str.substring(1)
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
