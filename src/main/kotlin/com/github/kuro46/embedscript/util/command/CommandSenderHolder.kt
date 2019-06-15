package com.github.kuro46.embedscript.util.command

import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandSenderHolder(
    val commandSender: CommandSender,
    private val cannotCastToPlayerMsg: String,
    private val cannotCastToConsoleMsg: String
) {
    fun tryCastToPlayerOrMessage(): Player? {
        return if (commandSender is Player) {
            commandSender
        } else {
            commandSender.sendMessage(cannotCastToPlayerMsg)
            null
        }
    }

    fun tryCastToConsoleOrMessage(): ConsoleCommandSender? {
        return if (commandSender is ConsoleCommandSender) {
            commandSender
        } else {
            commandSender.sendMessage(cannotCastToConsoleMsg)
            null
        }
    }
}
