package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.Script
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ListMultimap
import java.util.HashSet
import java.util.UUID

class ScriptBuilder private constructor(val author: UUID) {
    val moveTypes: MutableSet<Script.MoveType> = HashSet()
    val clickTypes: MutableSet<Script.ClickType> = HashSet()
    val pushTypes: MutableSet<Script.PushType> = HashSet()
    val script: ListMultimap<String, String> = ArrayListMultimap.create()

    fun build(createdAt: Long): Script {
        return Script(author, createdAt,
                ImmutableSet.copyOf<Script.MoveType>(moveTypes),
                ImmutableSet.copyOf<Script.ClickType>(clickTypes),
                ImmutableSet.copyOf<Script.PushType>(pushTypes),
                ImmutableListMultimap.copyOf(script))
    }

    companion object {

        fun withAuthor(author: UUID): ScriptBuilder {
            return ScriptBuilder(author)
        }
    }
}
