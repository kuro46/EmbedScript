package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.processor.executor.ScriptExecutor
import com.github.kuro46.embedscript.script.processor.parser.ScriptParser
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * @author shirokuro
 */
class ScriptProcessor(val logger: Logger, val plugin: Plugin, val configuration: Configuration) {
    private val processors: MutableMap<String, ChildProcessor> = ConcurrentHashMap()
    val scriptParser = ScriptParser(this)
    val scriptExecutor = ScriptExecutor(this)

    init {

        registerProcessor(Processors.LISTEN_CLICK_PROCESSOR)
        registerProcessor(Processors.LISTEN_MOVE_PROCESSOR)
        registerProcessor(Processors.LISTEN_PUSH_PROCESSOR)
        registerProcessor(Processors.BROADCAST_PROCESSOR)
        registerProcessor(Processors.BROADCAST_JSON_PROCESSOR)
        registerProcessor(Processors.SAY_PROCESSOR)
        registerProcessor(Processors.SAY_JSON_PROCESSOR)
        registerProcessor(Processors.NEEDED_PERMISSION_PROCESSOR)
        registerProcessor(Processors.UNNEEDED_PERMISSION_PROCESSOR)
        registerProcessor(Processors.COMMAND_PROCESSOR)
        registerProcessor(Processors.CONSOLE_PROCESSOR)
        GivePermissionProcessor.register(this)
        PresetProcessor.register(this)
    }

    fun registerProcessor(processor: ChildProcessor): ChildProcessor? {
        return processors.put(processor.key, processor)
    }

    fun unregisterProcessor(processor: ChildProcessor): ChildProcessor? {
        return processors.remove(processor.key)
    }

    fun isProcessorRegistered(processor: ChildProcessor): Boolean {
        return processors.containsKey(processor.key)
    }

    fun getProcessors(): Map<String, ChildProcessor> {
        return processors
    }

    // PARSE START

    fun parse(author: UUID, script: String, createdAt: Long = System.currentTimeMillis()): Script {
        return scriptParser.parse(author, script, createdAt)
    }

    /**
     * Prepare to build
     *
     * @param script script that ready to build
     * @throws ParseException If parse failed
     */
    fun prepareBuild(script: MutableScript) {
        scriptParser.prepareBuild(script)
    }

    // EXECUTE START

    fun execute(trigger: Player, scripts: List<Script>, scriptPosition: ScriptPosition) {
        scriptExecutor.execute(trigger, scripts, scriptPosition)
    }
}
