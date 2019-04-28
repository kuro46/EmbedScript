package com.github.kuro46.embedscript.script.processor.executor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.script.processor.ChildProcessor
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import com.github.kuro46.embedscript.util.Scheduler
import com.github.kuro46.embedscript.util.Util
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.StringJoiner
import java.util.logging.Logger

/**
 * @author shirokuro
 */
class ScriptExecutor(private val scriptProcessor: ScriptProcessor) {
    private val processors: Map<String, ChildProcessor>
        get() = scriptProcessor.getProcessors()
    private val configuration: Configuration
        get() = scriptProcessor.configuration
    private val logger: Logger
        get() = scriptProcessor.logger

    fun execute(trigger: Player, scripts: List<Script>, scriptPosition: ScriptPosition) {
        Scheduler.execute { scripts.forEach { executeAsync(trigger, it, scriptPosition) } }
    }

    private fun executeAsync(trigger: Player, script: Script, scriptPosition: ScriptPosition) {
        val executors = generateExecutors(trigger, script)

        try {
            // check phase
            for ((key, value) in executors) {
                val checkResult = executeInServerThreadIfNeeded(key.executionMode) {
                    key.check(trigger, value)
                }
                if (!checkResult) {
                    return
                }
            }

            // prepare phase
            executors.forEach { (executor, matchedValues) ->
                executeInServerThreadIfNeeded(executor.executionMode) {
                    executor.prepareExecute(trigger, matchedValues)
                }
            }
            // execute start
            executors.forEach { (executor, matchedValues) ->
                executeInServerThreadIfNeeded(executor.executionMode) {
                    executor.beginExecute(trigger, matchedValues)
                }
            }
        } finally {
            // execute end
            executors.forEach { (executor, matchedValues) ->
                executeInServerThreadIfNeeded(executor.executionMode) {
                    executor.endExecute(trigger, matchedValues)
                }
            }
        }

        if (configuration.isLogEnabled) {
            log(trigger, script, scriptPosition)
        }
    }

    private fun generateExecutors(trigger: Player, script: Script): List<Pair<ChildExecutor, List<String>>> {
        val executors: MutableList<Pair<ChildExecutor, List<String>>> = ArrayList()
        val scriptMap = script.script
        for (key in scriptMap.keySet()) {
            val values = scriptMap.get(key).map {
                // replace place holders
                var varIt = it
                varIt = replaceAndUnescape(varIt, "<player>") { trigger.name }
                varIt = replaceAndUnescape(varIt, "<world>") { trigger.world.name }
                varIt
            }

            val executorForValue = processors.getValue(key).executor

            executors.add(Pair(executorForValue, values))
        }

        return executors
    }

    private fun log(trigger: Player, script: Script, scriptPosition: ScriptPosition) {
        var message = configuration.logFormat
        message = replaceAndUnescape(message!!, "<trigger>") { trigger.name }
        message = replaceAndUnescape(message, "<script>") {
            val joiner = StringJoiner(" ")
            val scriptMap = script.script
            for (key in scriptMap.keySet()) {
                joiner.add("@$key ${ScriptUtil.toString(scriptMap.get(key))}")
            }
            joiner.toString()
        }
        val location = trigger.location
        val worldName = location.world.name
        message = replaceAndUnescape(message, "<trigger_world>") { worldName }
        message = replaceAndUnescape(message, "<trigger_x>") { location.blockX.toString() }
        message = replaceAndUnescape(message, "<trigger_y>") { location.blockY.toString() }
        message = replaceAndUnescape(message, "<trigger_z>") { location.blockZ.toString() }
        message = replaceAndUnescape(message, "<script_world>") { worldName }
        message = replaceAndUnescape(message, "<script_x>") { scriptPosition.x.toString() }
        message = replaceAndUnescape(message, "<script_y>") { scriptPosition.y.toString() }
        message = replaceAndUnescape(message, "<script_z>") { scriptPosition.z.toString() }

        logger.info(message)
    }


    private fun <T> executeInServerThreadIfNeeded(executionMode: ExecutionMode, function: () -> T): T {
        return when(executionMode) {
            ExecutionMode.SYNCHRONOUS -> {
                Bukkit.getScheduler().callSyncMethod(scriptProcessor.plugin, function).get()
            }
            ExecutionMode.ASYNCHRONOUS -> function()
        }
    }

    private fun replaceAndUnescape(source: String, target: String, messageFactory: () -> String): String {
        return if (!source.contains(target)) {
            source
        } else Util.replaceAndUnescape(source, target, messageFactory())

    }
}
