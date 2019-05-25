package com.github.kuro46.embedscript.script

import java.util.UUID

/**
 * @author shirokuro
 */
class Script(
        val createdAt: Long,
        val author: UUID,
        val keys: List<ParentKeyData>,
        val clickTypes: Set<ClickType>,
        val moveTypes: Set<MoveType>,
        val pushTypes: Set<PushType>
)

data class KeyData(val key: AbsoluteKey, val values: List<String>)

data class ParentKeyData(val key: AbsoluteKey, val values: List<String>, val children: List<KeyData>) : Iterable<KeyData> {
    override fun iterator(): Iterator<KeyData> {
        val base = ArrayList<KeyData>(children.size + 1)
        base.add(KeyData(key, values))
        base.addAll(children)

        return base.iterator()
    }

    companion object {
        fun fromMap(from: Map<String, List<String>>): List<ParentKeyData> {
            // fill parents
            val parents = LinkedHashMap<String, ParentKeyData>()
            for ((key, values) in from) {
                if (key.contains('.')) {
                    continue
                }
                parents[key] = ParentKeyData(key, values, emptyList())
            }

            // fill children
            for ((key, values) in from) {
                if (!key.contains('.')) {
                    continue
                }
                val split = key.split('.', limit = 2)
                val parentKey = split[0]

                val parent = parents.getValue(parentKey)
                val added = ArrayList<KeyData>(parent.children.size + 1)
                added.addAll(parent.children)
                added.add(KeyData(key, values))

                parents.replace(parentKey, parent.copy(children = added))
            }

            return ArrayList(parents.values)
        }
    }
}
