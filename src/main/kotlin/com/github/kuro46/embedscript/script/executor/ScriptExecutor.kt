package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.AbsoluteKey
import com.github.kuro46.embedscript.script.ExecutionMode
import com.github.kuro46.embedscript.script.KeyData
import com.github.kuro46.embedscript.script.ParentKeyData
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.parser.ScriptBuilder
import com.github.kuro46.embedscript.script.parser.StringParser
import com.github.kuro46.embedscript.util.PlaceholderData
import com.github.kuro46.embedscript.util.Replacer
import com.github.kuro46.embedscript.util.Scheduler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.StringJoiner
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * @author shirokuro
 */
class ScriptExecutor(
    val logger: Logger,
    val plugin: Plugin,
    val configuration: Configuration
) {
    private val executors = ConcurrentHashMap<AbsoluteKey, Pair<ExecutionMode, Executor>>()
    private val childExecutors = ConcurrentHashMap<AbsoluteKey, Executor>()
    private val parsers = ConcurrentHashMap<AbsoluteKey, Parser>()
    val executionLogger = ExecutionLogger(logger, configuration)
    val scriptReplacer = Replacer<Player>()

    init {
        Executors.registerAll(this)
        // dummy parser for preset feature
        registerParser("preset", object : Parser {
            override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
                // do-nothing
            }
        })

        scriptReplacer.add(PlaceholderData("<player>") { it.name })
        scriptReplacer.add(PlaceholderData("<world>") { it.world.name })
    }

    fun getKeys(): Set<String> {
        return parsers.keys
    }

    fun execute(player: Player, scripts: List<Script>, position: ScriptPosition) {
        if (scripts.isEmpty()) {
            return
        }

        Scheduler.execute {
            for (script in scripts) {
                execute(player, script, position)
            }
        }
    }

    private fun execute(player: Player, script: Script, position: ScriptPosition) {
        val task = Task()

        try {
            for (parentKeyData in script.keys) {
                val result = executeKeyData(task, player, compileKeyData(player, parentKeyData))
                if (result == ExecutionResult.CANCEL) {
                    return
                }
            }
        } finally {
            for ((mode, listener) in task.endListeners) {
                try {
                    runByExecMode(mode!!, listener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        executionLogger.log(LogData(player, position, script))
    }

    private fun executeKeyData(rootTask: Task, player: Player, keyData: ParentKeyData): ExecutionResult {
        val childTask = Task()

        val (executionMode, parentExecutor) = executors.getValue(keyData.key)

        return runByExecMode(executionMode) {
            try {
                for (childKeyData in keyData.children) {
                    val childExecutor = childExecutors.getValue(childKeyData.key)

                    val result = childExecutor.execute(childTask, player, childKeyData.values)
                    if (result == ExecutionResult.CANCEL) {
                        return@runByExecMode ExecutionResult.CONTINUE
                    }
                }

                return@runByExecMode parentExecutor.execute(rootTask, player, keyData.values)
            } finally {
                for ((mode, listener) in childTask.endListeners) {
                    if (mode != null) {
                        throw IllegalStateException()
                    }

                    try {
                        listener()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun compileKeyData(player: Player, parentKeyData: ParentKeyData): ParentKeyData {
        fun compileValues(values: List<String>): List<String> {
            return values.map {
                scriptReplacer.execute(it, player)
            }
        }

        val children = parentKeyData.children.map { KeyData(it.key, compileValues(it.values)) }
        return ParentKeyData(parentKeyData.key, compileValues(parentKeyData.values), children)
    }

    fun registerExecutor(
        key: String,
        executionMode: ExecutionMode,
        executor: Executor,
        parser: Parser = DEFAULT_PARSER
    ) {
        if (key.contains('.')) {
            throw IllegalArgumentException("Please don't use '.' for key of registerExecutor")
        }

        val lowerCasedKey = key.toLowerCase()
        executors[lowerCasedKey] = Pair(executionMode, executor)
        parsers[lowerCasedKey] = parser
    }

    fun registerChildExecutor(parentKey: String, key: String, executor: Executor, parser: Parser = DEFAULT_PARSER) {
        if (parentKey.contains('.')) {
            throw IllegalArgumentException("Please don't use '.' for key of registerChildExecutor")
        }

        val lowerCasedKey = "${parentKey.toLowerCase()}.${key.toLowerCase()}"
        childExecutors[lowerCasedKey] = executor
        parsers[lowerCasedKey] = parser
    }

    fun registerParser(key: String, parser: Parser) {
        if (key.contains('.')) {
            throw IllegalArgumentException("Please don't use '.' for key of registerParser")
        }

        parsers[key.toLowerCase()] = parser
    }

    fun registerChildParser(parentKey: String, key: String, parser: Parser) {
        if (parentKey.contains('.')) {
            throw IllegalArgumentException("Please don't use '.' for key of registerChildExecutor")
        }

        val lowerCasedKey = "${parentKey.toLowerCase()}.${key.toLowerCase()}"
        parsers[lowerCasedKey] = parser
    }

    private fun <T> runByExecMode(executionMode: ExecutionMode, function: () -> T): T {
        return when (executionMode) {
            ExecutionMode.SYNCHRONOUS -> Bukkit.getScheduler().callSyncMethod(plugin, function).get()
            ExecutionMode.ASYNCHRONOUS -> function()
        }
    }

    fun parse(createdAt: Long, author: UUID, stringScript: String): Script {
        val preparsed = StringParser.parse(applyPreset(stringScript))
        val builder = ScriptBuilder()
        for ((key, values) in preparsed) {
            parsers.getValue(key).parse(key, values, builder)
        }
        return builder.build(author, createdAt)
    }

    companion object {
        private val DEFAULT_PARSER = object : Parser {
            override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
                parseTo.flatRootEntry.getOrPut(key) { ArrayList() }.addAll(parseFrom)
            }
        }
    }

    private fun applyPreset(target: String): String {
        val parsed = StringParser.parse(target)
        val presets = parsed["preset"] ?: return target// @preset is not present

        val applied = StringJoiner(" ")
        for (preset in presets) {
            applied.add(configuration.presets!!.getValue(preset))
        }
        applied.add(target)

        return applied.toString()
    }
}
