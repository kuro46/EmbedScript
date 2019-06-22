package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.script.Author
import com.github.kuro46.embedscript.script.ExecutionMode
import com.github.kuro46.embedscript.script.ParentOption
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.parser.ScriptBuilder
import com.github.kuro46.embedscript.script.parser.StringParser
import com.github.kuro46.embedscript.util.PlaceholderData
import com.github.kuro46.embedscript.util.Replacer
import com.github.kuro46.embedscript.util.Scheduler
import java.util.StringJoiner
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author shirokuro
 */
class ScriptProcessor(val embedScript: EmbedScript) {
    val keys = ConcurrentHashMap<String, Option>()
    val executionLogger = ExecutionLogger(embedScript.logger, embedScript.configuration)
    val scriptReplacer = Replacer<Player>()

    init {
        // dummy
        registerKey(
            Option.parent(
                key = "preset",
                parser = Parser.NO_OP_PARSER
            )
        )
        Executors.registerAll(this)

        scriptReplacer.add(PlaceholderData("<player>") { it.name })
        scriptReplacer.add(PlaceholderData("<world>") { it.world.name })
    }

    fun registerKey(option: Option) {
        keys[option.key] = option
    }

    fun parse(createdAt: Long, author: Author, stringScript: String): Script {
        val preparsed = StringParser.parse(applyPreset(stringScript))
        val optionList = ParentOption.fromMap(preparsed)

        val builder = ScriptBuilder()

        for (option in optionList) {
            keys.getValue(option.key).parser.parse(option.key, option.values, builder)

            for (child in option.children) {
                keys.getValue(child.key).parser.parse(child.key, child.values, builder)
            }
        }

        return builder.build(author, createdAt)
    }

    private fun applyPreset(target: String): String {
        val parsed = StringParser.parse(target)
        val presets = parsed["preset"] ?: return target // @preset is not present

        val applied = StringJoiner(" ")
        for (preset in presets) {
            applied.add(embedScript.configuration.presets.getValue(preset))
        }
        applied.add(target)

        return applied.toString()
    }

    fun execute(player: Player, scripts: List<Script>, position: ScriptPosition) {
        if (scripts.isEmpty()) {
            return
        }

        Scheduler.execute { scripts.forEach { executeScript(player, it, position) } }
    }

    private fun executeScript(player: Player, script: Script, position: ScriptPosition) {
        val listeners = ArrayList<Pair<ExecutionMode, EndListener>>()

        try {
            for (scriptParentOption in script.keys) {
                val (executorData, result) = processKey(
                    player,
                    compileOption(player, scriptParentOption)
                )
                result.endListener?.let { listeners.add(Pair(executorData.executionMode, it)) }

                if (result.action == AfterExecuteAction.STOP) {
                    return
                }
            }
        } finally {
            for ((mode, listener) in listeners) {
                runCatching { runByExecMode(mode, listener) }.exceptionOrNull()?.printStackTrace()
            }
        }

        executionLogger.log(LogData(player, position, script))
    }

    private fun compileOption(player: Player, parentOption: ParentOption): ParentOption {
        fun compileValues(values: List<String>): List<String> {
            return values.map {
                scriptReplacer.execute(it, player)
            }
        }

        val children = parentOption.children.map { com.github.kuro46.embedscript.script.Option(it.key, compileValues(it.values)) }
        return ParentOption(parentOption.key, compileValues(parentOption.values), children)
    }

    private fun processKey(player: Player, parentOption: ParentOption): Pair<ExecutorData.ParentExecutorData, ExecutionResult> {
        val parentExecutorData =
            keys.getValue(parentOption.key).executorData
                ?.let { it as ExecutorData.ParentExecutorData }
                ?: throw IllegalStateException("ExecutionData for '${parentOption.key}' not found!")

        val result = runByExecMode(parentExecutorData.executionMode) {
            val listeners = ArrayList<EndListener>()

            try {
                for (child in parentOption.children) {
                    val childExecutorData =
                        keys.getValue(child.key).executorData
                            ?.let { it as ExecutorData.ChildExecutorData }
                            ?: throw IllegalStateException("ExecutionData for '${child.key}' not found!")
                    val result = childExecutorData.executor.execute(player, child.values)

                    result.endListener?.let { listeners.add(it) }

                    if (result.action == AfterExecuteAction.STOP) {
                        return@runByExecMode ExecutionResult(AfterExecuteAction.CONTINUE)
                    }
                }

                return@runByExecMode parentExecutorData.executor.execute(player, parentOption.values)
            } finally {
                for (listener in listeners) {
                    runCatching(listener).exceptionOrNull()?.printStackTrace()
                }
            }
        }

        return Pair(parentExecutorData, result)
    }

    private fun <T> runByExecMode(executionMode: ExecutionMode, function: () -> T): T {
        return when (executionMode) {
            ExecutionMode.SYNCHRONOUS -> Bukkit.getScheduler().callSyncMethod(embedScript.plugin, function).get()
            ExecutionMode.ASYNCHRONOUS -> function()
        }
    }
}
