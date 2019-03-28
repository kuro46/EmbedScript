package com.github.kuro46.embedscript.script.processor

import com.google.common.collect.ImmutableList
import org.bukkit.entity.Player

interface Processor {

    val key: String

    val omittedKey: String

    val parser: Parser

    val executor: Executor

    interface Parser {

        fun prepareBuild(processor: ScriptProcessor, script: MutableScript, key: String, matchedValues: ImmutableList<String>)

        fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>)
    }

    interface Executor {

        fun check(trigger: Player, matchedValues: ImmutableList<String>): Boolean

        fun prepareExecute(trigger: Player, matchedValues: ImmutableList<String>)

        fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>)

        fun endExecute(trigger: Player, matchedValues: ImmutableList<String>)
    }
}
