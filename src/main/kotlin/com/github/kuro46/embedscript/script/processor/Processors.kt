package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.google.common.collect.ImmutableList
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.stream.Collectors

object Processors {
    val DEFAULT_EXECUTOR: Processor.Executor = object : AbstractExecutor() {

    }
    val DEFAULT_PARSER: Processor.Parser = object : AbstractParser() {

    }

    // ONLY TO PARSE
    val LISTEN_CLICK_PROCESSOR = newProcessor("listen-click", "lc",
        object : AbstractParser() {
            override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
                addEnumToCollection(builder.clickTypes, Script.ClickType::class.java, matchedValues)
            }
        },
        DEFAULT_EXECUTOR)
    val LISTEN_MOVE_PROCESSOR = newProcessor("listen-move", "lm",
        object : AbstractParser() {
            override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
                addEnumToCollection(builder.moveTypes, Script.MoveType::class.java, matchedValues)
            }
        },
        DEFAULT_EXECUTOR)
    val LISTEN_PUSH_PROCESSOR = newProcessor("listen-push", "lm",
        object : AbstractParser() {
            override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
                addEnumToCollection(builder.pushTypes, Script.PushType::class.java, matchedValues)
            }
        },
        DEFAULT_EXECUTOR)

    // CHECK PHASE

    val NEEDED_PERMISSION_PROCESSOR = newProcessor("needed-permission", "np",
        DEFAULT_PARSER,
        NeededPermissionExecutor())
    val UNNEEDED_PERMISSION_PROCESSOR = newProcessor("unneeded-permission", "up",
        DEFAULT_PARSER,
        object : NeededPermissionExecutor() {
            override fun check(trigger: Player, matchedValues: ImmutableList<String>): Boolean {
                // invert
                return !super.check(trigger, matchedValues)
            }
        })

    // EXECUTION PHASE

    private val COMMAND_PARSER = object : AbstractParser() {
        override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
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

    val COMMAND_PROCESSOR = newProcessor("command", "c",
        COMMAND_PARSER,
        object : AbstractExecutor() {
            override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {
                matchedValues.forEach { trigger.performCommand(it) }
            }
        })
    val CONSOLE_PROCESSOR = newProcessor("console", "con",
        COMMAND_PARSER,
        object : AbstractExecutor() {
            override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {
                matchedValues.forEach { string -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string) }
            }
        })
    val SAY_PROCESSOR = newProcessor("say", "s",
        DEFAULT_PARSER,
        object : AbstractExecutor() {
            override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {
                matchedValues.forEach { trigger.sendMessage(it) }
            }
        })
    val SAY_JSON_PROCESSOR = newProcessor("say-json", "sj",
        DEFAULT_PARSER,
        object : AbstractExecutor() {
            override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {
                matchedValues.forEach { string -> trigger.spigot().sendMessage(*ComponentSerializer.parse(string)) }
            }
        })
    val BROADCAST_PROCESSOR = newProcessor("broadcast", "b",
        DEFAULT_PARSER,
        object : AbstractExecutor() {
            override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {
                Bukkit.getOnlinePlayers().forEach { player ->
                    for (string in matchedValues) {
                        player.sendMessage(string)
                    }
                }
            }
        })
    val BROADCAST_JSON_PROCESSOR = newProcessor("broadcast-json", "bj",
        DEFAULT_PARSER,
        object : AbstractExecutor() {
            override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {
                val jsonMessages: List<Array<BaseComponent>> = matchedValues.stream()
                    .map { json -> ComponentSerializer.parse(json) }
                    .collect(Collectors.toList())
                Bukkit.getOnlinePlayers().forEach { player -> jsonMessages.forEach { baseComponents -> player.spigot().sendMessage(*baseComponents) } }
            }
        })

    fun newProcessor(key: String,
                     omittedKey: String,
                     parser: Processor.Parser,
                     executor: Processor.Executor): Processor {
        return object : Processor {
            override val key: String
                get() = key

            override val omittedKey: String
                get() = omittedKey

            override val parser: Processor.Parser
                get() = parser

            override val executor: Processor.Executor
                get() = executor
        }
    }

    private fun <T : Enum<T>> addEnumToCollection(collection: MutableCollection<T>,
                                                  clazz: Class<T>,
                                                  strings: List<String>) {
        for (string in strings) {
            try {
                collection.add(java.lang.Enum.valueOf<T>(clazz, string.toUpperCase(Locale.ENGLISH)))
            } catch (e: IllegalArgumentException) {
                throw ParseException("'$string' is unavailable value!")
            }

        }
    }

    private open class NeededPermissionExecutor : AbstractExecutor() {
        override fun check(trigger: Player, matchedValues: ImmutableList<String>): Boolean {
            for (matchedValue in matchedValues) {
                if (!trigger.hasPermission(matchedValue)) {
                    return false
                }
            }
            return true
        }
    }
}
