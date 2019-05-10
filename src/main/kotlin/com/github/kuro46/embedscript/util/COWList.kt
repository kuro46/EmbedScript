package com.github.kuro46.embedscript.util


class COWList<T>(private val delegate: List<T>) : AbstractMutableList<T>() {
    private var mutable: MutableList<T>? = null

    override val size: Int
        get() = getPreferredList().size

    private fun getPreferredList(): List<T> {
        return mutable ?: delegate
    }

    private fun getMutableList(): MutableList<T> {
        val mutableList = if (mutable != null) {
            mutable
        } else {
            mutable = delegate.toMutableList()
            mutable
        }
        return mutableList!!
    }

    override fun add(index: Int, element: T) {
        getMutableList().add(index, element)
    }

    override fun get(index: Int): T {
        return getPreferredList()[index]
    }

    override fun removeAt(index: Int): T {
        return getMutableList().removeAt(index)
    }

    override fun set(index: Int, element: T): T {
        return getMutableList().set(index, element)
    }

    companion object {
        private val EMPTY_COW_LIST = COWList<Nothing>(emptyList())

        fun <T> empty(): COWList<T> = EMPTY_COW_LIST as COWList<T>
    }
}
