package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.script.parser.ScriptBuilder

/**
 * @author shirokuro
 */
interface Parser {

    fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder)

    companion object {
        val DEFAULT_PARSER = object : Parser {
            override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
                parseTo.flatRootEntry[key] = parseFrom.toMutableList()
            }
        }

        val NO_OP_PARSER = object : Parser {
            override fun parse(key: String, parseFrom: List<String>, parseTo: ScriptBuilder) {
            }
        }
    }
}
