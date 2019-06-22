package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.Author
import com.github.kuro46.embedscript.script.EventType
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptUtils
import com.github.kuro46.embedscript.script.executor.ScriptProcessor
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandHandlerManager
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.ExecutionThreadType
import com.github.kuro46.embedscript.util.command.LongArgumentInfo
import org.bukkit.entity.Player

class SBCommandHandler(
    commandHandlerManager: CommandHandlerManager,
    private val eventType: EventType,
    private val processor: ScriptProcessor,
    private val requests: Requests
) {
    init {
        val commandName = "sb" + eventType.name.toLowerCase()
        commandHandlerManager.registerHandler(
            "$commandName add",
            SBActionHandler(
                { scripts -> Request.Add(scripts) },
                "Add a script to the clicked block. (Uses ScriptBlock's format.')"
            )
        )
        commandHandlerManager.registerHandler(
            "$commandName create",
            SBActionHandler(
                { scripts -> Request.Embed(scripts) },
                "Embed a script to the clicked block. (Uses ScriptBlock's format.')"
            )
        )
        commandHandlerManager.registerAlias(
            "$commandName remove",
            "embedscript remove"
        )
        commandHandlerManager.registerAlias(
            "$commandName view",
            "embedscript view"
        )
    }

    private fun queueRequest(player: Player, request: Request) {
        player.sendMessage(Prefix.INFO + "Please click any block...")
        requests.putRequest(player, request)
    }

    private fun createScriptOrNull(player: Player, stringScript: String): List<Script>? {
        return try {
            ScriptUtils.createScriptFromLegacyFormat(
                processor,
                Author.Player(player.uniqueId),
                eventType,
                stringScript
            )
        } catch (e: ParseException) {
            player.sendMessage(
                Prefix.ERROR + "Failed to parse the script: ${e.message}"
            )
            null
        }
    }

    private inner class SBActionHandler(
        val requestFactory: (List<Script>) -> Request,
        commandDescription: String
    ) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            LongArgumentInfo(
                "script",
                true
            )
        ),
        commandDescription
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val player = senderHolder.tryCastToPlayerOrMessage() ?: return

            val scripts =
                createScriptOrNull(player, args.getValue("script")) ?: return

            queueRequest(player, requestFactory(scripts))
        }
    }

    companion object {
        fun register(
            commandHandlerManager: CommandHandlerManager,
            eventType: EventType,
            processor: ScriptProcessor,
            requests: Requests
        ) {
            SBCommandHandler(
                commandHandlerManager,
                eventType,
                processor,
                requests
            )
        }
    }
}
