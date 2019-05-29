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
object Registrator {
    fun register(executor: ScriptExecutor) {
        val execModeForPermOP = if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            ExecutionMode.ASYNCHRONOUS
        } else {
            ExecutionMode.SYNCHRONOUS
        }

        executor.registerExecutor("neededPerm", execModeForPermOP, NEEDED_PERMISSION_EXECUTOR)
        executor.registerExecutor("unneededPerm", execModeForPermOP, UNNEEDED_PERMISSION_EXECUTOR)
        executor.registerExecutor("cmd", ExecutionMode.SYNCHRONOUS, COMMAND_EXECUTOR, COMMAND_PARSER)
        executor.registerChildExecutor("cmd", "bypass", CommandBypassExecutor(executor.plugin))
        executor.registerExecutor("console", ExecutionMode.SYNCHRONOUS, CONSOLE_EXECUTOR)
        executor.registerExecutor("say", ExecutionMode.ASYNCHRONOUS, SAY_EXECUTOR)
        executor.registerExecutor("sayRaw", ExecutionMode.ASYNCHRONOUS, SAY_RAW_EXECUTOR)
        executor.registerExecutor("broadcast", ExecutionMode.ASYNCHRONOUS, BROADCAST_EXECUTOR)
        executor.registerExecutor("broadcastRaw", ExecutionMode.ASYNCHRONOUS, BROADCAST_RAW_EXECUTOR)

        executor.registerParser("listenClick", LISTEN_CLICK_PARSER)
        executor.registerParser("listenMove", LISTEN_MOVE_PARSER)
        executor.registerParser("listenPush", LISTEN_PUSH_PARSER)
    }

    val NEEDED_PERMISSION_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            for (value in values) {
                if (!player.hasPermission(value)) {
                    return ExecutionResult.CANCEL
                }
            }
            return ExecutionResult.CONTINUE
        }
    }

    val UNNEEDED_PERMISSION_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            for (value in values) {
                if (player.hasPermission(value)) {
                    return ExecutionResult.CANCEL
                }
            }
            return ExecutionResult.CONTINUE
        }
    }

    val COMMAND_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            values.forEach { player.performCommand(it) }
            return ExecutionResult.CONTINUE
        }
    }

    val CONSOLE_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            values.forEach { string -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string) }
            return ExecutionResult.CONTINUE
        }
    }

    val SAY_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            values.forEach { player.sendMessage(it) }
            return ExecutionResult.CONTINUE
        }
    }

    val SAY_RAW_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            values.forEach { string -> player.spigot().sendMessage(*ComponentSerializer.parse(string)) }
            return ExecutionResult.CONTINUE
        }
    }

    val BROADCAST_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            Bukkit.getOnlinePlayers().forEach { sendTo ->
                for (string in values) {
                    sendTo.sendMessage(string)
                }
            }
            return ExecutionResult.CONTINUE
        }
    }

    val BROADCAST_RAW_EXECUTOR = object : Executor() {
        override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
            values.stream()
                .map { json -> ComponentSerializer.parse(json) }
                .forEach {
                    Bukkit.getOnlinePlayers().forEach { sendTo -> sendTo.spigot().sendMessage(*it) }
                }
            return ExecutionResult.CONTINUE
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
    override fun execute(task: Task, player: Player, values: List<String>): ExecutionResult {
        if (values.isEmpty()) {
            return ExecutionResult.CONTINUE
        }

        val attachment = player.addAttachment(plugin)

        task.onEnd {
            attachment.remove()
        }

        for (value in values) {
            if (player.hasPermission(value)) {
                continue
            }
            attachment.setPermission(value, true)
        }

        return ExecutionResult.CONTINUE
    }
}
