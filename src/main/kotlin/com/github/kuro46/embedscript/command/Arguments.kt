package com.github.kuro46.embedscript.command

import org.bukkit.command.CommandSender

class Arguments(raw: List<String>) : List<String> by raw {
    fun getInt(sender: CommandSender, index: Int): Int? {
        return getOrNull(index)?.let {
            val intOrNull = it.toIntOrNull()
            if (intOrNull == null) {
                sender.sendMessage("'$it' is not a valid number!")
            }
            intOrNull
        }
    }

    fun isElementEnough(maxIndex: Int): Boolean {
        return lastIndex >= maxIndex
    }

    fun isElementNotEnough(maxIndex: Int): Boolean {
        return !isElementEnough(maxIndex)
    }
}
