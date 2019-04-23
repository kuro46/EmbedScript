package com.github.kuro46.embedscript.script.processor.parser

import com.github.kuro46.embedscript.script.processor.MutableScript
import com.github.kuro46.embedscript.script.processor.ScriptBuilder
import com.github.kuro46.embedscript.script.processor.ScriptProcessor

interface Parser {

    fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: List<String>)

    fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>)
}
