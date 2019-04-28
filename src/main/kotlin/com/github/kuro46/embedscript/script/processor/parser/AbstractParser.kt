package com.github.kuro46.embedscript.script.processor.parser

import com.github.kuro46.embedscript.script.processor.MutableScript
import com.github.kuro46.embedscript.script.processor.ScriptBuilder
import com.github.kuro46.embedscript.script.processor.ScriptProcessor

/**
 * @author shirokuro
 */
abstract class AbstractParser : ChildParser {
    override fun prepareBuild(
            processor: ScriptProcessor,
            script: MutableScript,
            key: String,
            matchedValues: List<String>
    ) {

    }

    override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
        builder.script.putAll(key, matchedValues)
    }
}
