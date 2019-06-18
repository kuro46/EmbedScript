package com.github.kuro46.embedscript.util.command

import com.github.kuro46.embedscript.Prefix
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.Plugin

/**
 * A class for manages command handler.
 *
 * Thread-Safety
 *
 * @author shirokuro
 *
 * @constructor
 *
 * @param executor Executor for asynchronous command handling.
 * @param plugin Plugin instance for synchronous command handling.
 */
class CommandHandlerManager(
    private val executor: Executor,
    private val plugin: Plugin
) : CommandExecutor, TabCompleter {
    /**
     * List of handlers.
     */
    private val handlers = ConcurrentHashMap<String, CommandHandler>()

    /**
     * Register a new handler for command.
     *
     * @param command command string (e.g. "embedscript embed")
     * @param handler handler to register
     */
    fun registerHandler(command: String, handler: CommandHandler) {
        @Suppress("NAME_SHADOWING")
        val command = command.toLowerCase()

        handlers[command] = handler

        val commandName = getCommandName(command)
        Bukkit.getPluginCommand(commandName).executor = this
        if (!handlers.containsKey(commandName)) {
            handlers[commandName] = RootCommandHandler(commandName)
        }
        if (!handlers.containsKey("$commandName help")) {
            handlers["$commandName help"] = HelpCommandHandler(commandName)
        }
    }

    fun registerAlias(command: String, aliasOf: String) {
        registerHandler(command, handlers.getValue(aliasOf))
    }

    /**
     * Returns command name of specified command with args
     *
     * @param commandWithArgs command with args
     */
    private fun getCommandName(commandWithArgs: String): String {
        return commandWithArgs.split(' ')[0]
    }

    /**
     * Please don't call this method because this is internal function.
     */
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        executor.execute {
            val commandWithArgs = appendCommandAndArgs(command.name, args.toList())
            val commandSenderHolder = holdCommandSender(sender)
            val (handlerCommand, handler, notParsedArgs) = getCommandData(commandWithArgs)
            val parseResult = handler.argumentInfoList.parse(notParsedArgs)
            if (parseResult is ParseResult.Success) {
                handler.executionThreadType.executeAtSyncOrCurrentThread(plugin) {
                    handler.handleCommand(commandSenderHolder, parseResult.arguments)
                }
            } else if (parseResult is ParseResult.Failed) {
                val errorType = parseResult.errorType
                when (errorType) {
                    ErrorType.TOO_MANY_ARGUMENTS -> {
                        sender.sendMessage(Prefix.ERROR + "Too many arguments!")
                    }
                    ErrorType.REQUIED_ARGUMENT_NOT_ENOUGH -> {
                        sender.sendMessage(Prefix.ERROR + "Arguments not enough.")
                    }
                }
                sender.sendMessage(Prefix.ERROR + "Usage: " + handlerToString(handlerCommand, handler))
            } else {
                throw IllegalStateException()
            }
        }

        return true
    }

    /**
     * Please don't call this method becasuse this is internal function.
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        val commandWithArgs = appendCommandAndArgs(command.name, args.toList())
        val commandSenderHolder = holdCommandSender(sender)
        val (_, handler, argsForHandler) = getCommandData(commandWithArgs)

        @Suppress("NAME_SHADOWING")
        val spaceFiltered = argsForHandler.filter { it.isNotEmpty() }

        val (uncompletedArg, completedArgs) = if (spaceFiltered.isEmpty()) {
            Pair("", emptyList())
        } else {
            Pair(spaceFiltered.last(), spaceFiltered.dropLast(1))
        }

        return handler.handleTabComplete(commandSenderHolder, command.name, completedArgs, uncompletedArg)
    }

    /**
     * Creates a CommandSenderHolder from specified CommandSender.
     *
     * @param sender CommandSender
     */
    private fun holdCommandSender(
        sender: CommandSender
    ): CommandSenderHolder {
        return CommandSenderHolder(
            commandSender = sender,
            cannotCastToPlayerMsg = Prefix.ERROR + "Cannot perform this command from the console.",
            cannotCastToConsoleMsg = Prefix.ERROR + "Cannot perform this command from the player."
        )
    }

    /**
     * Appends command and args.
     *
     * @param command command
     * @param args arguments
     */
    private fun appendCommandAndArgs(
        command: String,
        args: List<String>
    ): List<String> {
        return ArrayList<String>(args.size + 1).apply {
            add(command)
            addAll(args)
        }
    }

    /**
     * Returns the command handler and arguments for that
     * from specified command with arguments.
     *
     * @param commandWithArgs command with arguments
     */
    private fun getCommandData(
        commandWithArgs: List<String>
    ): Triple<String, CommandHandler, List<String>> {
        var handler: CommandHandler? = null
        var handlerString: String? = null
        val argsForHandler = ArrayList<String>()
        val consumedElements = StringBuilder()
        for (element in commandWithArgs) {
            if (consumedElements.isNotEmpty()) {
                consumedElements.append(' ')
            }
            consumedElements.append(element)
            val nullableHandler = handlers[consumedElements.toString().toLowerCase()]
            if (nullableHandler != null) {
                argsForHandler.clear()
                handler = nullableHandler
                handlerString = consumedElements.toString().toLowerCase()
            }

            if (nullableHandler == null && handler != null) {
                argsForHandler.add(element)
            }
        }

        handler ?: throw IllegalStateException("handler for '${commandWithArgs.joinToString(" ")}' not found!")

        return Triple(handlerString!!, handler, argsForHandler)
    }

    private fun handlerToString(
        commandOfHandler: String,
        handler: CommandHandler
    ): String {
        val requiedArgs = requiedArgInfoListToString(handler.argumentInfoList.requied)
        val lastArg = lastArgInfoToString(handler.argumentInfoList.last)

        val builder =
            StringBuilder()
                .append(ChatColor.GOLD)
                .append('/')
                .append(commandOfHandler)
                .append(ChatColor.GRAY)

        if (requiedArgs.isNotEmpty()) {
            builder.append(' ')
            builder.append(requiedArgs)
        }

        if (lastArg.isNotEmpty()) {
            builder.append(' ')
            builder.append(lastArg)
        }

        builder.append(ChatColor.RESET)

        return builder.toString()
    }

    private fun requiedArgInfoListToString(requiedArgInfoList: List<RequiedArgumentInfo>): String {
        val stringBuilder = StringBuilder()
        for (info in requiedArgInfoList) {
            if (stringBuilder.isNotEmpty()) {
                stringBuilder.append(' ')
            }
            stringBuilder.append("<${info.name}>")
        }
        return stringBuilder.toString()
    }

    private fun lastArgInfoToString(lastArgInfo: LastArgument): String {
        return when (lastArgInfo) {
            is LastArgument.NotAllow -> ""
            is OptionalArguments -> {
                val stringBuilder = StringBuilder()
                for (info in lastArgInfo.arguments) {
                    if (stringBuilder.isNotEmpty()) {
                        stringBuilder.append(' ')
                    }
                    stringBuilder.append("[${info.name}]")
                }
                stringBuilder.toString()
            }
            is LongArgumentInfo -> {
                if (lastArgInfo.requied) {
                    "<${lastArgInfo.name}>"
                } else {
                    "${lastArgInfo.name}]"
                }
            }
        }
    }

    private inner class RootCommandHandler(private val commandName: String) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            LongArgumentInfo("dummy", false)
        )
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            senderHolder.commandSender.sendMessage(
                Prefix.ERROR + "Incorrect usage. Please try perform '/$commandName help'."
            )
        }

        override fun handleTabComplete(
            senderHolder: CommandSenderHolder,
            commandName: String,
            completedArgs: List<String>,
            uncompletedArg: String
        ): List<String> {
            val targetString = commandName + " " + completedArgs.joinToString(" ") + uncompletedArg
            val neededIndex = completedArgs.lastIndex + 1 /* for 'commandName' */ + 1 /* for 'uncompletedArg' */
            val suggestions = HashSet<String>()
            handlers.keys
                .filter { it.startsWith(targetString) }
                .forEach { suggestions.add(it.split(' ')[neededIndex]) }
            return suggestions.toList()
        }
    }

    private inner class HelpCommandHandler(val commandName: String) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            LastArgument.NotAllow
        ),
        "Displays help message."
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val sender = senderHolder.commandSender

            sender.sendMessage(Prefix.INFO + "Help for '/$commandName'")
            handlers.forEach { (command, handler) ->
                if (command.startsWith(commandName)) {
                    if (handler is RootCommandHandler) {
                        return@forEach
                    }

                    val argumentInfoList = handler.argumentInfoList
                    val argumentString = StringBuilder(requiedArgInfoListToString(argumentInfoList.requied))
                    if (argumentString.isNotEmpty()) argumentString.append(' ')
                    argumentString.append(lastArgInfoToString(argumentInfoList.last))
                    if (!argumentString.endsWith(' ')) argumentString.append(' ')
                    if (!argumentString.startsWith(' ')) argumentString.insert(0, ' ')
                    sender.sendMessage(Prefix.INFO + "${handlerToString(command, handler)} ${ChatColor.YELLOW}- ${ChatColor.RESET}${handler.description}")
                }
            }
        }
    }
}
