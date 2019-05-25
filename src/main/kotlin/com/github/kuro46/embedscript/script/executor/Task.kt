package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.script.ExecutionMode

/**
 * @author shirokuro
 */
class Task {

    val endListeners: MutableList<ListenerData> = ArrayList()

    fun onEnd(executionMode: ExecutionMode? = null, listener: () -> Unit) {
        endListeners.add(ListenerData(executionMode, listener))
    }

    data class ListenerData(val executionMode: ExecutionMode?, val listener: () -> Unit)
}
