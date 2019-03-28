package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.ParseException
import com.google.common.collect.ImmutableList

class PresetProcessor(configuration: Configuration) : Processor {
    override val parser: Processor.Parser

    override val key: String
        get() = "preset"

    override val omittedKey: String
        get() = "p"

    override val executor: Processor.Executor
        get() = Processors.DEFAULT_EXECUTOR

    init {
        this.parser = PresetParser(configuration)
    }

    private class PresetParser(private val configuration: Configuration) : AbstractParser() {

        @Throws(ParseException::class)
        override fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: ImmutableList<String>) {
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

        @Throws(ParseException::class)
        override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
            // do nothing
            // please do not remove this method because AbstractParser#build does builder.getScript().putAll(key, matchedValues);
        }
    }
}
