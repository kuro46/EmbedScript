package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.ParseException
import com.google.common.collect.ImmutableList

abstract class AbstractParser : Processor.Parser {
    @Throws(ParseException::class)
    override fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: ImmutableList<String>) {

    }

    @Throws(ParseException::class)
    override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
        builder.script.putAll(key, matchedValues)
    }
}
