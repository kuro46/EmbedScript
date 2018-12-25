package com.github.kuro46.embedscript.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author shirokuro
 */
public final class Scheduler {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("EmbedScript Scheduler Thread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception in " + t.getName());
            e.printStackTrace();
        });

        return thread;
    });

    private Scheduler() {
    }

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

    public static Future<?> submit(Runnable task) {
        return EXECUTOR.submit(task);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return EXECUTOR.submit(task);
    }

    public static <T> Future<T> submit(Runnable task, T t) {
        return EXECUTOR.submit(task, t);
    }
}
