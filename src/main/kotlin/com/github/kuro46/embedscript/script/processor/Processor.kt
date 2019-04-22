package com.github.kuro46.embedscript.script.processor

import org.bukkit.entity.Player

interface Processor {

    val key: String

    val omittedKey: String

    val parser: Parser

    val executor: Executor

    interface Parser {

        fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: List<String>)

        fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>)
    }

    interface Executor {

        fun check(trigger: Player, matchedValues: List<String>): Boolean

        fun prepareExecute(trigger: Player, matchedValues: List<String>)

        fun beginExecute(trigger: Player, matchedValues: List<String>)

        fun endExecute(trigger: Player, matchedValues: List<String>)
    }
}
