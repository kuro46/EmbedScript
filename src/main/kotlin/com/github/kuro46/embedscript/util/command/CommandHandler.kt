package com.github.kuro46.embedscript.util.command

abstract class CommandHandler(
    val executionThreadType: ExecutionThreadType,
    val argumentInfoList: ArgumentInfoList
) {

    abstract fun handleCommand(
        senderHolder: CommandSenderHolder,
        args: Map<String, String>
    )

    open fun handleTabComplete(
        senderHolder: CommandSenderHolder,
        commandName: String,
        completedArgs: List<String>,
        uncompletedArg: String
    ): List<String> {
        return emptyList()
    }
}
