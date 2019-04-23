package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.processor.executor.Executor
import com.github.kuro46.embedscript.script.processor.parser.Parser

interface Processor {

    val key: String

    val omittedKey: String

    val parser: Parser

    val executor: Executor
}
