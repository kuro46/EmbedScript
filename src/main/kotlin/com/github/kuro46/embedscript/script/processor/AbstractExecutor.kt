package com.github.kuro46.embedscript.script.processor

import org.bukkit.entity.Player

abstract class AbstractExecutor : Processor.Executor {
    override fun check(trigger: Player, matchedValues: List<String>): Boolean {
        return true
    }

    override fun prepareExecute(trigger: Player, matchedValues: List<String>) {

    }

    override fun beginExecute(trigger: Player, matchedValues: List<String>) {

    }

    override fun endExecute(trigger: Player, matchedValues: List<String>) {

    }
}
