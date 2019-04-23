package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.processor.executor.Executor
import com.github.kuro46.embedscript.script.processor.parser.AbstractParser
import com.github.kuro46.embedscript.script.processor.parser.Parser

class PresetProcessor(configuration: Configuration) : Processor {
    override val parser: Parser = PresetParser(configuration)

    override val key = "preset"

    override val omittedKey = "p"

    override val executor: Executor = Processors.DEFAULT_EXECUTOR

    private class PresetParser(private val configuration: Configuration) : AbstractParser() {
        override fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: List<String>) {
            var mergeTo: MutableScript? = null
            val presets = configuration.presets
            for (value in matchedValues) {
                val preset = presets!![value]
                if (preset == null) {
                    throw ParseException("'$value' is unknown preset!")
                } else {
                    val mutableScript = MutableScript(preset)

                    processor.prepareBuild(mutableScript)

                    if (mergeTo == null) {
                        mergeTo = mutableScript
                    } else {
                        mergeTo.putAllMapBehavior(mutableScript)
                    }
                }
            }

            mergeTo!!.putAllMapBehavior(script)
            script.clear()
            mergeTo.getView().forEach { k, value -> script.add(k, value) }
        }

        override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
            // do nothing
            // please do not remove this method because AbstractParser#build does builder.getScript().putAll(key, matchedValues);
        }
    }
}
