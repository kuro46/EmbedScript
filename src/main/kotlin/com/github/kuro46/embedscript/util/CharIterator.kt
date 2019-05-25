package com.github.kuro46.embedscript.util

/**
 * @author shirokuro
 */
class CharIterator(source: String) : Iterator<Char> {
    private var index = -1
    val chars = source.toCharArray()

    override fun hasNext(): Boolean {
        val nextIndex = index + 1
        return chars.lastIndex >= nextIndex
    }

    override fun next(): Char {
        return chars[++index]
    }

    fun peekNext(): Char {
        return chars[index + 1]
    }

    fun peekPrev(): Char {
        return chars[index - 1]
    }

    fun current(): Char {
        return chars[index]
    }
}
