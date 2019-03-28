package com.github.kuro46.embedscript.script.processor

import com.google.common.collect.ImmutableList
import org.bukkit.entity.Player

abstract class AbstractExecutor : Processor.Executor {
    override fun check(trigger: Player, matchedValues: ImmutableList<String>): Boolean {
        return true
    }

    override fun prepareExecute(trigger: Player, matchedValues: ImmutableList<String>) {

    }

    override fun beginExecute(trigger: Player, matchedValues: ImmutableList<String>) {

    }

    override fun endExecute(trigger: Player, matchedValues: ImmutableList<String>) {

    }
}
