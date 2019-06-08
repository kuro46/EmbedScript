package com.github.kuro46.embedscript.script.executor

import org.bukkit.entity.Player

/**
 * @author shirokuro
 */
abstract class Executor {
    abstract fun execute(player: Player, values: List<String>): ExecutionResult
}

class ExecutionResultBuilder {
    var endListener: EndListener? = null

    fun build(action: AfterExecuteAction): ExecutionResult {
        return ExecutionResult(action, endListener)
    }
}

class ExecutionResult(
    val action: AfterExecuteAction,
    var endListener: EndListener? = null
)

typealias EndListener = () -> Unit

enum class AfterExecuteAction {
    /**
     * Continue the execution after execute
     */
    CONTINUE,
    /**
     * Stop the execution after execute
     */
    STOP
}
