package com.github.kuro46.embedscript.script.processor.executor

import org.bukkit.entity.Player

interface Executor {

    fun check(trigger: Player, matchedValues: List<String>): Boolean

    fun prepareExecute(trigger: Player, matchedValues: List<String>)

    fun beginExecute(trigger: Player, matchedValues: List<String>)

    fun endExecute(trigger: Player, matchedValues: List<String>)
}
