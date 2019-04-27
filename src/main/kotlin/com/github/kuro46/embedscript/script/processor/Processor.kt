package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.processor.executor.ChildExecutor
import com.github.kuro46.embedscript.script.processor.parser.ChildParser

interface Processor {

    val key: String

    val omittedKey: String

    val parser: ChildParser

    val executor: ChildExecutor
}
