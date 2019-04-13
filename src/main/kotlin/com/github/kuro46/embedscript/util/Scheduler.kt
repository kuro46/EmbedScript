package com.github.kuro46.embedscript.util

import java.util.concurrent.Executors

/**
 * @author shirokuro
 */
object Scheduler {
    private val EXECUTOR = Executors.newCachedThreadPool { r ->
        val thread = Thread(r)
        thread.name = "EmbedScript Scheduler Thread"
        thread.isDaemon = true

        thread
    }

    fun execute(task: () -> Unit) {
        EXECUTOR.execute(task)
    }
}
