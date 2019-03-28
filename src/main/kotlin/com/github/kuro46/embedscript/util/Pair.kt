package com.github.kuro46.embedscript.util

import java.util.*

class Pair<K, V>(val key: K, val value: V) {

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val pair = o as Pair<*, *>?
        return key == pair!!.key && value == pair.value
    }

    override fun hashCode(): Int {
        return Objects.hash(key, value)
    }
}
