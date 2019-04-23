package com.github.kuro46.embedscript.script.processor.parser

import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.processor.MutableScript
import com.github.kuro46.embedscript.script.processor.Processor
import com.github.kuro46.embedscript.script.processor.ScriptBuilder
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import java.util.HashMap
import java.util.HashSet
import java.util.UUID

class ScriptParser(private val scriptProcessor: ScriptProcessor) {
    private val processors: Map<String, Processor>
        get() = scriptProcessor.getProcessors()

    fun parse(author: UUID, script: String, createdAt: Long = System.currentTimeMillis()): Script {
        val mutableScript = MutableScript(script)
        prepareBuild(mutableScript)
        return buildScript(createdAt, author, mutableScript)
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
            val processor = processors.getValue(key)
            processor.parser.prepareBuild(scriptProcessor, script, key, view.get(key))
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
    private fun buildScript(createdAt: Long, author: UUID, script: MutableScript): Script {
        val builder = ScriptBuilder.withAuthor(author)
        val view = script.getView()
        for ((key) in view.entries()) {
            val processor = processors[key]!!
            processor.parser.build(builder, key, view.get(key))
        }

        return builder.build(createdAt)
    }
}
