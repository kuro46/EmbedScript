package com.github.kuro46.embedscript.script.executor

import org.bukkit.entity.Player

/**
 * @author shirokuro
 */
abstract class Executor {
    abstract fun execute(task: Task, player: Player, values: List<String>): ExecutionResult
}

enum class ExecutionResult {
    CONTINUE,
    CANCEL
}
