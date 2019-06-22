package com.github.kuro46.embedscript.script

/**
 * @author shirokuro
 */
class Script(
    val createdAt: Long,
    val author: Author,
    val keys: List<ParentOption>,
    val clickTypes: Set<ClickType>,
    val moveTypes: Set<MoveType>,
    val pushTypes: Set<PushType>
)

data class Option(val key: AbsoluteKey, val values: List<String>)

data class ParentOption(val key: AbsoluteKey, val values: List<String>, val children: List<Option>) :
    Iterable<Option> {
    override fun iterator(): Iterator<Option> {
        val base = ArrayList<Option>(children.size + 1)
        base.add(Option(key, values))
        base.addAll(children)

        return base.iterator()
    }

    companion object {
        fun fromMap(from: Map<String, List<String>>): List<ParentOption> {
            // fill parents
            val parents = LinkedHashMap<String, ParentOption>()
            for ((key, values) in from) {
                if (key.contains('.')) {
                    continue
                }
                parents[key] = ParentOption(key, values, emptyList())
            }

            // fill children
            for ((key, values) in from) {
                if (!key.contains('.')) {
                    continue
                }
                val split = key.split('.', limit = 2)
                val parentKey = split[0]

                val parent = parents.getValue(parentKey)
                val added = ArrayList<Option>(parent.children.size + 1)
                added.addAll(parent.children)
                added.add(Option(key, values))

                parents.replace(parentKey, parent.copy(children = added))
            }

            return ArrayList(parents.values)
        }
    }
}
