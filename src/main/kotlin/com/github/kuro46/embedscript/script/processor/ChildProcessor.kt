package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.processor.executor.ChildExecutor
import com.github.kuro46.embedscript.script.processor.parser.ChildParser

/**
 * @author shirokuro
 */
data class ChildProcessor(val key: String,
                          val omittedKey: String,
                          val parser: ChildParser,
                          val executor: ChildExecutor)
