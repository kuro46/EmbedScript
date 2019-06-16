package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.script.executor.ScriptProcessor
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.ExecutionThreadType

/**
 * @author shirokuro
 */
abstract class AbstractScriptCommandHandler(
    private val scriptProcessor: ScriptProcessor,
    executionThreadType: ExecutionThreadType,
    argumentInfoList: ArgumentInfoList,
    description: String = "Description not present."
) : CommandHandler(executionThreadType, argumentInfoList, description) {

    abstract override fun handleCommand(senderHolder: CommandSenderHolder, args: Map<String, String>)

    override fun handleTabComplete(
        senderHolder: CommandSenderHolder,
        commandName: String,
        completedArgs: List<String>,
        uncompletedArg: String
    ): List<String> {
        return if (isKey(completedArgs)) {
            // uncompleted arg is key
            scriptProcessor.keys.keys.map { "@$it" }
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
