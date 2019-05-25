package com.github.kuro46.embedscript.util

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps

object ArrayListLinkedMultimap {
    fun <K, V> create(): ListMultimap<K, V> {
        return Multimaps.newListMultimap(LinkedHashMap()) { ArrayList<V>() }
    }
}
