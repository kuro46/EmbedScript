package com.github.kuro46.embedscript.util

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

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

    fun submit(task: Runnable): Future<*> {
        return EXECUTOR.submit(task)
    }

    fun <T> submit(task: Callable<T>): Future<T> {
        return EXECUTOR.submit(task)
    }

    fun <T> submit(task: Runnable, t: T): Future<T> {
        return EXECUTOR.submit(task, t)
    }
}
