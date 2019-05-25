package com.github.kuro46.embedscript.script.parser

import com.github.kuro46.embedscript.util.CharIterator

/**
 * @author shirokuro
 */
class StringParser private constructor(target: String) {
    private val result = LinkedHashMap<String, List<String>>()
    private val keyBuf = StringBuilder()
    private val valueBuf = StringBuilder()

    init {
        val iterator = CharIterator(target)

        var state = State.OTHER

        for (char in iterator) {
            when (state) {
                State.KEY -> keyBuf.append(char)
                State.VALUE -> valueBuf.append(char)
                State.OTHER -> flushIfNeeded()
            }

            state = getNextState(state, iterator)
        }

        flushIfNeeded()
    }

    private fun flushIfNeeded() {
        if (keyBuf.isNotEmpty() && valueBuf.isNotEmpty()) {
            result[keyBuf.toString().toLowerCase()] =
                    ValueParser.parse(valueBuf.toString())

            keyBuf.clear()
            valueBuf.clear()
        }
    }

    private fun getNextState(currentState: State, iterator: CharIterator): State {
        if (!iterator.hasNext()) {
            return State.OTHER
        }

        // key to other
        if (currentState == State.KEY && iterator.peekNext() == ' ') {
            return State.OTHER
        }
        // other to key
        if (currentState == State.OTHER && iterator.current() == '@') {
            return State.KEY
        }
        // value to other
        if (currentState == State.VALUE
                && iterator.peekPrev() != '\\'
                && iterator.current() == ']'
                && iterator.peekNext() == ' '
        ) {
            return State.OTHER
        }
        // other to value
        if (currentState == State.OTHER && iterator.peekNext() == '[') {
            return State.VALUE
        }

        return currentState
    }

    companion object {
        fun parse(target: String): Map<String, List<String>> {
            return StringParser(target).result
        }
    }

    enum class State {
        KEY,
        VALUE,
        OTHER
    }
}

private class ValueParser private constructor(target: String) {
    private val result = ArrayList<String>()
    private val buffer = StringBuilder()

    init {
        val iterator = CharIterator(target)

        var state = State.DELIMITER

        for (char in iterator) {
            when (state) {
                State.VALUE -> buffer.append(char)
                State.EMPTY_VALUE -> result.add("")
                State.DELIMITER -> flushIfNeeded()
            }

            state = getNextState(state, iterator)
        }

        flushIfNeeded()
    }

    private fun flushIfNeeded() {
        if (buffer.isNotEmpty()) {
            result.add(buffer.toString())
            buffer.clear()
        }
    }

    private fun getNextState(currentState: State, iterator: CharIterator): State {
        if (!iterator.hasNext()) {
            return State.DELIMITER
        }

        if (currentState == State.EMPTY_VALUE) {
            return State.DELIMITER
        }

        if (currentState == State.DELIMITER
                && iterator.current() == '['
        ) {

            return if (iterator.peekNext() == ']') {
                State.EMPTY_VALUE
            } else {
                State.VALUE
            }
        }
        if (currentState == State.VALUE
                && iterator.current() != '\\'
                && iterator.peekNext() == ']'
        ) {
            return State.DELIMITER
        }

        return currentState
    }

    enum class State {
        DELIMITER,
        VALUE,
        EMPTY_VALUE
    }

    companion object {
        fun parse(target: String): List<String> {
            return ValueParser(target).result
        }
    }
}
