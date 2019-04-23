package com.github.kuro46.embedscript.script.processor.executor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.script.processor.Processor
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import com.github.kuro46.embedscript.util.Scheduler
import com.github.kuro46.embedscript.util.Util
import org.bukkit.entity.Player
import java.util.StringJoiner
import java.util.logging.Logger
import java.util.stream.Collectors

class ScriptExecutor(private val scriptProcessor: ScriptProcessor) {
    private val processors: Map<String, Processor>
        get() = scriptProcessor.getProcessors()
    private val configuration: Configuration
        get() = scriptProcessor.configuration
    private val logger: Logger
        get() = scriptProcessor.logger

    fun execute(trigger: Player, script: Script, scriptPosition: ScriptPosition) {
        val executors: MutableList<Pair<Executor, List<String>>> = ArrayList()
        val scriptMap = script.script
        for (key in scriptMap.keySet()) {
            val value = scriptMap.get(key).stream()
                    .map { string ->
                        var s = string
                        s = replaceAndUnescape(s, "<player>") { trigger.name }
                        s = replaceAndUnescape(s, "<world>") { trigger.world.name }
                        s
                    }
                    .collect(Collectors.toList())
            executors.add(Pair(processors.getValue(key).executor, value))
        }

        try {
            // check phase
            for ((key, value) in executors) {
                if (!key.check(trigger, value)) {
                    return
                }
            }

            // prepare phase
            executors.forEach { (executor, matchedValues) -> executor.prepareExecute(trigger, matchedValues) }
            // execute start
            executors.forEach { (executor, matchedValues) -> executor.beginExecute(trigger, matchedValues) }
        } finally {
            // execute end
            executors.forEach { (executor, matchedValues) -> executor.endExecute(trigger, matchedValues) }
        }

        if (configuration.isLogEnabled) {
            Scheduler.execute {
                var message = configuration.logFormat
                message = replaceAndUnescape(message!!, "<trigger>") { trigger.name }
                message = replaceAndUnescape(message, "<script>") {
                    val joiner = StringJoiner(" ")
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
        }
    }

    private fun replaceAndUnescape(source: String, target: String, messageFactory: () -> String): String {
        return if (!source.contains(target)) {
            source
        } else Util.replaceAndUnescape(source, target, messageFactory())

    }
}
