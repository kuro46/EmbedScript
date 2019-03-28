package com.github.kuro46.embedscript.script

import java.util.*

object ScriptUtil {

    fun toString(values: Iterable<String>): String {
        val joiner = StringJoiner("][", "[", "]")
        for (value in values) {
            joiner.add(value)
        }
        return joiner.toString()
    }

    fun toString(value: String): String {
        return "[$value]"
    }
}
