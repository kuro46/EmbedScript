package com.github.kuro46.embedscript.script.parser

import com.github.kuro46.embedscript.script.ClickType
import com.github.kuro46.embedscript.script.MoveType
import com.github.kuro46.embedscript.script.ParentKeyData
import com.github.kuro46.embedscript.script.PushType
import com.github.kuro46.embedscript.script.Script
import java.util.UUID

/**
 * @author shirokuro
 */
class ScriptBuilder(
    val flatRootEntry: MutableMap<String, MutableList<String>> = LinkedHashMap(),
    val moveTypes: MutableSet<MoveType> = HashSet(),
    val clickTypes: MutableSet<ClickType> = HashSet(),
    val pushTypes: MutableSet<PushType> = HashSet()
) {
    fun build(author: UUID, createdAt: Long = System.currentTimeMillis()): Script {
        val keys = ParentKeyData.fromMap(flatRootEntry)

        return Script(
            createdAt,
            author,
            keys,
            clickTypes,
            moveTypes,
            pushTypes
        )
    }
}
