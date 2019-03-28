package com.github.kuro46.embedscript.util

import java.util.*

class Pair<K, V>(val key: K, val value: V) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val pair = other as Pair<*, *>?
        return key == pair!!.key && value == pair.value
    }

    override fun hashCode(): Int {
        return Objects.hash(key, value)
    }
}
