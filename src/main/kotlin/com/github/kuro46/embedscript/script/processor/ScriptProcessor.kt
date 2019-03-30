package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.util.Scheduler
import com.github.kuro46.embedscript.util.Util
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.logging.Logger
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class ScriptProcessor(private val logger: Logger, plugin: Plugin, private val configuration: Configuration) {
    private val processors = HashMap<String, Processor>()

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
        registerProcessor(GivePermissionProcessor(plugin, configuration))
        registerProcessor(PresetProcessor(configuration))
    }

    fun registerProcessor(processor: Processor): Processor? {
        return processors.put(processor.key, processor)
    }

    fun unregisterProcessor(processor: Processor): Processor? {
        return processors.remove(processor.key)
    }

    fun isProcessorRegistered(processor: Processor): Boolean {
        return processors.containsKey(processor.key)
    }

    // PARSE START

    fun parse(author: UUID, script: String): Script {
        val mutableScript = MutableScript(script)
        prepareBuild(mutableScript)
        return buildScript(author, mutableScript)
    }

    /**
     * Prepare to build
     *
     * @param script script that ready to build
     * @throws ParseException If parse failed
     */
    fun prepareBuild(script: MutableScript) {
        canonicalizeBuffer(script)
        val view = script.getView()
        for (key in view.keySet()) {
            val processor = processors[key]!!
            processor.parser.prepareBuild(this, script, key, view.get(key))
        }
    }

    /**
     * Canonicalize the keys e.g. 'p' to 'preset'
     *
     * @param script script to canonicalize
     * @throws ParseException If processor for key not exists
     */
    private fun canonicalizeBuffer(script: MutableScript) {
        // generate lookup table
        val omittedKeys = HashMap<String, String>()
        val keys = HashSet<String>()
        for (processor in processors.values) {
            omittedKeys[processor.omittedKey] = processor.key
            keys.add(processor.key)
        }

        for ((key, value) in script.getView().entries()) {
            if (omittedKeys.containsKey(key)) {
                script.replaceKey(value, key, omittedKeys[key]!!)
                continue
            }

            if (!keys.contains(key)) {
                throw ParseException("'$key' is unknown key!")
            }
        }
    }

    /**
     * Build the script from MutableScript
     *
     * @param author author of this script
     * @param script modifiable script
     * @return script
     */
    private fun buildScript(author: UUID, script: MutableScript): Script {
        val builder = ScriptBuilder.withAuthor(author)
        val view = script.getView()
        for ((key) in view.entries()) {
            val processor = processors[key]!!
            processor.parser.build(builder, key, view.get(key))
        }

        return builder.build(System.currentTimeMillis())
    }

    // EXECUTE START

    fun execute(trigger: Player, script: Script, scriptPosition: ScriptPosition) {
        val executors: MutableList<Pair<Processor.Executor, List<String>>> = ArrayList()
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
            executors.add(Pair(processors[key]!!.executor, value))
        }

        try {
            // check phase
            for ((key, value) in executors) {
                if (!key.check(trigger, value)) {
                    return
                }
            }

            // prepare phase
            executors.forEach { (executor, matchedValues) -> executor.prepareExecute(trigger, matchedValues)}
            // execute start
            executors.forEach { (executor, matchedValues) -> executor.beginExecute(trigger, matchedValues)}
        } finally {
            // execute end
            executors.forEach { (executor, matchedValues) -> executor.endExecute(trigger, matchedValues)}
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
