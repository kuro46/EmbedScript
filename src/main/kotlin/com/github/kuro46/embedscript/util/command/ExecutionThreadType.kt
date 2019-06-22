package com.github.kuro46.embedscript.util.command

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

enum class ExecutionThreadType {
    SYNCHRONOUS {
        override fun executeAtSyncOrCurrentThread(
            plugin: Plugin,
            function: () -> Unit
        ) {
            Bukkit.getScheduler().callSyncMethod(plugin, function).get()
        }
    },
    ASYNCHRONOUS {
        override fun executeAtSyncOrCurrentThread(
            plugin: Plugin,
            function: () -> Unit
        ) {
            function()
        }
    };

    abstract fun executeAtSyncOrCurrentThread(
        plugin: Plugin,
        function: () -> Unit
    )
}
