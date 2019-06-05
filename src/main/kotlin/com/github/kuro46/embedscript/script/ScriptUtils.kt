package com.github.kuro46.embedscript.script

/**
 * @author shirokuro
 */
object ScriptUtils {

    fun toString(values: Iterable<String>): String {
        return values.joinToString("][", "[", "]")
    }

    fun toString(value: String): String {
        return "[$value]"
    }
}
