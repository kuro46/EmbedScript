package com.github.kuro46.embedscript.script.processor.parser

import com.github.kuro46.embedscript.script.processor.MutableScript
import com.github.kuro46.embedscript.script.processor.ScriptBuilder
import com.github.kuro46.embedscript.script.processor.ScriptProcessor

/**
 * @author shirokuro
 */
interface ChildParser {

    fun getSuggestions(uncompletedArg: String): List<String> = emptyList()

    fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: List<String>)

    fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>)
}
