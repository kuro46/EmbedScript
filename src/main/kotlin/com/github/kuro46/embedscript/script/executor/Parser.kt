package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.script.parser.ScriptBuilder

/**
 * @author shirokuro
 */
interface Parser {

    fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder)
}
