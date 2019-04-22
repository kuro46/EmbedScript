package com.github.kuro46.embedscript.script.processor

abstract class AbstractParser : Processor.Parser {
    override fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: List<String>) {

    }

    override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
        builder.script.putAll(key, matchedValues)
    }
}
