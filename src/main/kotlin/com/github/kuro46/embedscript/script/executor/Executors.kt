package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.script.ExecutionMode
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.parser.ScriptBuilder
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.Locale
import java.util.stream.Collectors

/**
 * @author shirokuro
 */
object Executors {
    fun registerAll(processor: ScriptProcessor) {
        val execModeForPermOP = if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            ExecutionMode.ASYNCHRONOUS
        } else {
            ExecutionMode.SYNCHRONOUS
        }

        processor.registerKey(KeyData.parent(
            "neededPerm",
            ExecutorData.parent(execModeForPermOP, NEEDED_PERMISSION_EXECUTOR)
        ))
        processor.registerKey(KeyData.parent(
            "unneededPerm",
            ExecutorData.parent(execModeForPermOP, UNNEEDED_PERMISSION_EXECUTOR)
        ))
        processor.registerKey(KeyData.parent(
            "cmd",
            ExecutorData.parent(execModeForPermOP, COMMAND_EXECUTOR),
            COMMAND_PARSER
        ))
        processor.registerKey(KeyData.child(
            "cmd.bypass",
            ExecutorData.child(CommandBypassExecutor(processor.embedScript.plugin))
        ))
        processor.registerKey(KeyData.parent(
            "console",
            ExecutorData.parent(ExecutionMode.SYNCHRONOUS, CONSOLE_EXECUTOR),
            COMMAND_PARSER
        ))
        processor.registerKey(KeyData.parent(
            "say",
            ExecutorData.parent(ExecutionMode.ASYNCHRONOUS, SAY_EXECUTOR)
        ))
        processor.registerKey(KeyData.parent(
            "sayRaw",
            ExecutorData.parent(ExecutionMode.ASYNCHRONOUS, SAY_RAW_EXECUTOR)
        ))
        processor.registerKey(KeyData.parent(
            "broadcast",
            ExecutorData.parent(ExecutionMode.ASYNCHRONOUS, BROADCAST_EXECUTOR)
        ))
        processor.registerKey(KeyData.parent(
            "broadcastRaw",
            ExecutorData.parent(ExecutionMode.ASYNCHRONOUS, BROADCAST_RAW_EXECUTOR)
        ))
        processor.registerKey(KeyData.parent(
            key = "listenClick",
            parser = LISTEN_CLICK_PARSER
        ))
        processor.registerKey(KeyData.parent(
            key = "listenMove",
            parser = LISTEN_MOVE_PARSER
        ))
        processor.registerKey(KeyData.parent(
            key = "listenPush",
            parser = LISTEN_PUSH_PARSER
        ))
    }

    val NEEDED_PERMISSION_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            for (value in values) {
                if (!player.hasPermission(value)) {
                    return ExecutionResult(AfterExecuteAction.STOP)
                }
            }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val UNNEEDED_PERMISSION_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            for (value in values) {
                if (player.hasPermission(value)) {
                    return ExecutionResult(AfterExecuteAction.STOP)
                }
            }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val COMMAND_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            values.forEach { player.performCommand(it) }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val CONSOLE_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            values.forEach { string -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string) }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val SAY_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            values.forEach { player.sendMessage(it) }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val SAY_RAW_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            values.forEach { string -> player.spigot().sendMessage(*ComponentSerializer.parse(string)) }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val BROADCAST_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            Bukkit.getOnlinePlayers().forEach { sendTo ->
                for (string in values) {
                    sendTo.sendMessage(string)
                }
            }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val BROADCAST_RAW_EXECUTOR = object : Executor() {
        override fun execute(player: Player, values: List<String>): ExecutionResult {
            values.stream()
                .map { json -> ComponentSerializer.parse(json) }
                .forEach {
                    Bukkit.getOnlinePlayers().forEach { sendTo -> sendTo.spigot().sendMessage(*it) }
                }
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }
    }

    val COMMAND_PARSER = object : Parser {
        override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
            val modifiedForCommand = parseFrom.stream()
                // remove slash char if needed
                .map { commandWithArgs ->
                    if (commandWithArgs.startsWith("/"))
                        commandWithArgs.substring(1)
                    else
                        commandWithArgs
                }
                // canonicalize the command
                .map { commandWithArgs ->
                    val splitCommandWithArgs = commandWithArgs.split(" ")
                    val pluginCommand = Bukkit.getPluginCommand(splitCommandWithArgs[0])
                    val canonicalizedCommand = if (pluginCommand == null)
                        splitCommandWithArgs[0]
                    else
                        pluginCommand.name
                    val args = splitCommandWithArgs.stream()
                        .skip(1)
                        .collect(Collectors.joining(" "))
                    "$canonicalizedCommand $args"
                }
                .collect(Collectors.toList())
            parseTo.flatRootEntry.getOrPut(key) { ArrayList() }.addAll(modifiedForCommand)
        }
    }

    val LISTEN_CLICK_PARSER = object : Parser {
        override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
            addEnumToCollection(parseTo.clickTypes, parseFrom)
        }
    }

    val LISTEN_MOVE_PARSER = object : Parser {
        override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
            addEnumToCollection(parseTo.moveTypes, parseFrom)
        }
    }

    val LISTEN_PUSH_PARSER = object : Parser {
        override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
            addEnumToCollection(parseTo.pushTypes, parseFrom)
        }
    }

    private inline fun <reified T : Enum<T>> addEnumToCollection(
        collection: MutableCollection<T>,
        strings: List<String>
    ) {
        for (string in strings) {
            try {
                collection.add(java.lang.Enum.valueOf<T>(T::class.java, string.toUpperCase(Locale.ENGLISH)))
            } catch (e: IllegalArgumentException) {
                throw ParseException("'$string' is unavailable value!")
            }
        }
    }
}


/**
 * @author shirokuro
 */
private class CommandBypassExecutor(val plugin: Plugin) : Executor() {
    override fun execute(player: Player, values: List<String>): ExecutionResult {
        if (values.isEmpty()) {
            return ExecutionResult(AfterExecuteAction.CONTINUE)
        }

        val attachment = player.addAttachment(plugin)

        val resultBuilder = ExecutionResultBuilder()
        resultBuilder.endListener = { attachment.remove() }

        for (value in values) {
            if (player.hasPermission(value)) {
                continue
            }
            attachment.setPermission(value, true)
        }

        return resultBuilder.build(AfterExecuteAction.CONTINUE)
    }
}
