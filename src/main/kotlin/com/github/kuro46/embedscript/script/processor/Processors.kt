package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.processor.executor.AbstractExecutor
import com.github.kuro46.embedscript.script.processor.executor.ChildExecutor
import com.github.kuro46.embedscript.script.processor.executor.ExecutionMode
import com.github.kuro46.embedscript.script.processor.parser.AbstractParser
import com.github.kuro46.embedscript.script.processor.parser.ChildParser
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Locale
import java.util.stream.Collectors

/**
 * @author shirokuro
 */
object Processors {
    val DEFAULT_EXECUTOR: ChildExecutor = object : AbstractExecutor() {
        override val executionMode: ExecutionMode
            get() = ExecutionMode.ASYNCHRONOUS
    }
    val DEFAULT_PARSER: ChildParser = object : AbstractParser() {

    }

    // ONLY TO PARSE
    val LISTEN_CLICK_PROCESSOR = ChildProcessor("listen-click", "lc",
            object : AbstractParser() {
                override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
                    addEnumToCollection(builder.clickTypes, matchedValues)
                }

                override fun getSuggestions(uncompletedArg: String): List<String> {
                    return Script.ClickType.values().map { it.name }
                }
            },
            DEFAULT_EXECUTOR)
    val LISTEN_MOVE_PROCESSOR = ChildProcessor("listen-move", "lm",
            object : AbstractParser() {
                override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
                    addEnumToCollection(builder.moveTypes, matchedValues)
                }

                override fun getSuggestions(uncompletedArg: String): List<String> {
                    return Script.MoveType.values().map { it.name }
                }
            },
            DEFAULT_EXECUTOR)
    val LISTEN_PUSH_PROCESSOR = ChildProcessor("listen-push", "lm",
            object : AbstractParser() {
                override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
                    addEnumToCollection(builder.pushTypes, matchedValues)
                }

                override fun getSuggestions(uncompletedArg: String): List<String> {
                    return Script.PushType.values().map { it.name }
                }
            },
            DEFAULT_EXECUTOR)

    // CHECK PHASE

    val PERMISSION_PARSER = object : AbstractParser() {
        override fun getSuggestions(uncompletedArg: String): List<String> {
            return Bukkit.getPluginManager().permissions.map { it.name }
        }
    }

    val NEEDED_PERMISSION_PROCESSOR = ChildProcessor("needed-permission", "np",
            PERMISSION_PARSER,
            NeededPermissionExecutor())
    val UNNEEDED_PERMISSION_PROCESSOR = ChildProcessor("unneeded-permission", "up",
            PERMISSION_PARSER,
            object : NeededPermissionExecutor() {
                override fun check(trigger: Player, matchedValues: List<String>): Boolean {
                    // invert
                    return !super.check(trigger, matchedValues)
                }
            })

    // EXECUTION PHASE

    private val COMMAND_PARSER = object : AbstractParser() {
        override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
            val modifiedForCommand = matchedValues.stream()
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
            builder.script.putAll(key, modifiedForCommand)
        }
    }

    val COMMAND_PROCESSOR = ChildProcessor("command", "c",
            COMMAND_PARSER,
            object : AbstractExecutor() {
                override val executionMode: ExecutionMode
                    get() = ExecutionMode.SYNCHRONOUS

                override fun beginExecute(trigger: Player, matchedValues: List<String>) {
                    matchedValues.forEach { trigger.performCommand(it) }
                }
            })
    val CONSOLE_PROCESSOR = ChildProcessor("console", "con",
            COMMAND_PARSER,
            object : AbstractExecutor() {
                override val executionMode: ExecutionMode
                    get() = ExecutionMode.SYNCHRONOUS

                override fun beginExecute(trigger: Player, matchedValues: List<String>) {
                    matchedValues.forEach { string -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string) }
                }
            })
    val SAY_PROCESSOR = ChildProcessor("say", "s",
            DEFAULT_PARSER,
            object : AbstractExecutor() {
                override val executionMode: ExecutionMode
                    get() = ExecutionMode.ASYNCHRONOUS

                override fun beginExecute(trigger: Player, matchedValues: List<String>) {
                    matchedValues.forEach { trigger.sendMessage(it) }
                }
            })
    val SAY_JSON_PROCESSOR = ChildProcessor("say-json", "sj",
            DEFAULT_PARSER,
            object : AbstractExecutor() {
                override val executionMode: ExecutionMode
                    get() = ExecutionMode.ASYNCHRONOUS

                override fun beginExecute(trigger: Player, matchedValues: List<String>) {
                    matchedValues.forEach { string -> trigger.spigot().sendMessage(*ComponentSerializer.parse(string)) }
                }
            })
    val BROADCAST_PROCESSOR = ChildProcessor("broadcast", "b",
            DEFAULT_PARSER,
            object : AbstractExecutor() {
                override val executionMode: ExecutionMode
                    get() = ExecutionMode.ASYNCHRONOUS

                override fun beginExecute(trigger: Player, matchedValues: List<String>) {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        for (string in matchedValues) {
                            player.sendMessage(string)
                        }
                    }
                }
            })
    val BROADCAST_JSON_PROCESSOR = ChildProcessor("broadcast-json", "bj",
            DEFAULT_PARSER,
            object : AbstractExecutor() {
                override val executionMode: ExecutionMode
                    get() = ExecutionMode.ASYNCHRONOUS

                override fun beginExecute(trigger: Player, matchedValues: List<String>) {
                    matchedValues.stream()
                            .map { json -> ComponentSerializer.parse(json) }
                            .forEach {
                                Bukkit.getOnlinePlayers().forEach { player -> player.spigot().sendMessage(*it) }
                            }
                }
            })

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

    private open class NeededPermissionExecutor : AbstractExecutor() {
        override val executionMode: ExecutionMode
            get() {
                return if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms"))
                    ExecutionMode.ASYNCHRONOUS
                else ExecutionMode.SYNCHRONOUS
            }
        override fun check(trigger: Player, matchedValues: List<String>): Boolean {
            for (matchedValue in matchedValues) {
                if (!trigger.hasPermission(matchedValue)) {
                    return false
                }
            }
            return true
        }
    }
}
