package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.script.ExecutionMode
import java.util.Locale

/**
 * Data of the key of script
 *
 * @author shirokuro
 */
sealed class Option(
    key: String,
    var executorData: ExecutorData?,
    var parser: Parser
) {
    val key: String = key.toLowerCase(Locale.ENGLISH)

    class ParentOption(
        key: String,
        executorData: ExecutorData.ParentExecutorData? = null,
        parser: Parser = Parser.DEFAULT_PARSER
    ) : Option(
        key,
        executorData,
        parser
    )

    class ChildOption(
        key: String,
        executorData: ExecutorData.ChildExecutorData? = null,
        parser: Parser = Parser.DEFAULT_PARSER
    ) : Option(
        key,
        executorData,
        parser
    )

    companion object {
        fun parent(
            key: String,
            executorData: ExecutorData.ParentExecutorData? = null,
            parser: Parser = Parser.DEFAULT_PARSER
        ): ParentOption {
            return ParentOption(key, executorData, parser)
        }

        fun child(
            key: String,
            executorData: ExecutorData.ChildExecutorData? = null,
            parser: Parser = Parser.DEFAULT_PARSER
        ): ChildOption {
            return ChildOption(key, executorData, parser)
        }
    }
}

/**
 * Data of the executor
 */
sealed class ExecutorData(val executor: Executor) {

    class ParentExecutorData(
        val executionMode: ExecutionMode,
        executor: Executor
    ) : ExecutorData(executor)

    class ChildExecutorData(executor: Executor) : ExecutorData(executor)

    companion object {
        fun parent(
            executionMode: ExecutionMode,
            executor: Executor
        ): ParentExecutorData {
            return ParentExecutorData(executionMode, executor)
        }

        fun child(executor: Executor): ChildExecutorData {
            return ChildExecutorData(executor)
        }
    }
}
