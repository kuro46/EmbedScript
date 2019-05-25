package com.github.kuro46.embedscript.util

import com.google.common.util.concurrent.Futures
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @author shirokuro
 */
object Scheduler {
    private val threads: MutableSet<Thread> = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))
    private val executor = Executors.newCachedThreadPool { r ->
        val thread = Thread(r)
        thread.name = "EmbedScript Scheduler Thread"
        thread.isDaemon = true

        threads.add(thread)

        thread
    }

    fun execute(forceOtherThread: Boolean = false, task: () -> Unit) {
        if (!forceOtherThread && threads.contains(Thread.currentThread())) {
            task()
        } else {
            executor.execute(wrapFunction(task))
        }
    }

    fun submit(forceOtherThread: Boolean = false, task: () -> Unit): Future<*> {
        return if (!forceOtherThread && threads.contains(Thread.currentThread())) {
            Futures.immediateFuture(null)
        } else {
            executor.submit(wrapFunction(task))
        }
    }

    private fun wrapFunction(task: () -> Unit): () -> Unit {
        return {
            try {
                task()
            } catch (e: Throwable) {
                System.err.println("Exception or Error occurred in '${Thread.currentThread().name}' !")
                e.printStackTrace()
            }
        }
    }
}
