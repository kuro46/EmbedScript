package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.EventType
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptUtils
import com.github.kuro46.embedscript.script.executor.ScriptProcessor
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandHandlerManager
import com.github.kuro46.embedscript.util.command.ExecutionThreadType
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.LastArgument
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.LongArgumentInfo
import org.bukkit.entity.Player

/**
 * CommandHandler for alias command (/esinteract, /eswalk)
 *
 * @author shirokuro
 */
object AliasCommandHandler {
    fun registerHandlers(
        manager: CommandHandlerManager,
        eventType: EventType,
        scriptProcessor: ScriptProcessor,
        requests: Requests
    ) {
        val rootCommand = eventType.commandName
        val scriptRequester = ScriptRequester(eventType.presetName, scriptProcessor, requests)

        manager.registerHandler("$rootCommand help", HelpHandler(eventType))
        manager.registerHandler("$rootCommand add", ModifyCommandHandler(
            ModifyRequestType.Add,
            scriptRequester
        ))
        manager.registerHandler("$rootCommand embed", ModifyCommandHandler(
            ModifyRequestType.Embed,
            scriptRequester
        ))
        manager.registerHandler("$rootCommand sbAdd", SBModifyCommandHandler(
            scriptRequester,
            ModifyRequestType.Add,
            eventType,
            scriptProcessor
        ))
        manager.registerHandler("$rootCommand sbCreate", SBModifyCommandHandler(
            scriptRequester,
            ModifyRequestType.Embed,
            eventType,
            scriptProcessor
        ))
    }
}

private class HelpHandler(private val eventType: EventType) : CommandHandler(
    ExecutionThreadType.ASYNCHRONOUS,
    ArgumentInfoList(emptyList(), LastArgument.NotAllow)
) {
    override fun handleCommand(
        senderHolder: CommandSenderHolder,
        args: Map<String, String>
    ) {
        @Suppress("NAME_SHADOWING")
        val command = "/${eventType.commandName}"
        senderHolder.commandSender.sendMessage("""
            $command help - Displays this message.
            $command add - Adds a script to the clicked block. (prefixed with '@preset [${eventType.presetName}]')
            $command embed - Embeds a script to the clicked block. (prefixed with '@preset [${eventType.presetName}]')
            $command sbAdd - Add a script to the clicked block. (similar to '/sb${eventType.name.toLowerCase()} add')
            $command sbCreate - Embeds a script to the clicked block. (similar to '/sb${eventType.name.toLowerCase()} create')
        """.trimIndent())
    }
}

private class SBModifyCommandHandler(
    private val scriptRequester: ScriptRequester,
    private val requestType: ModifyRequestType,
    private val eventType: EventType,
    private val processor: ScriptProcessor
) : CommandHandler(
    ExecutionThreadType.ASYNCHRONOUS,
    ArgumentInfoList(emptyList(), LongArgumentInfo("script", true))
) {
    override fun handleCommand(
        senderHolder: CommandSenderHolder,
        args: Map<String, String>
    ) {
        val player = senderHolder.tryCastToPlayerOrMessage() ?: return
        val script = try {
            ScriptUtils.createScriptFromLegacyFormat(
                processor,
                player.uniqueId,
                eventType,
                args.getValue("script")
            )
        } catch (e: ParseException) {
            player.sendMessage(Prefix.ERROR + "Failed to parse the script: ${e.message}")
            return
        }
        scriptRequester.request(player, script, requestType)
    }
}

private class ModifyCommandHandler(
    private val requestType: ModifyRequestType,
    private val scriptRequester: ScriptRequester
) : CommandHandler(
    ExecutionThreadType.ASYNCHRONOUS,
    ArgumentInfoList(emptyList(), LongArgumentInfo("script", true))
) {
    override fun handleCommand(
        senderHolder: CommandSenderHolder,
        args: Map<String, String>
    ) {
        val player = senderHolder.tryCastToPlayerOrMessage() ?: return
        scriptRequester.request(player, args.getValue("script"), requestType)
    }
}

private class ScriptRequester(
    private val presetName: String,
    private val scriptProcessor: ScriptProcessor,
    private val requests: Requests
) {
    fun request(
        player: Player,
        stringScript: String,
        modifyRequestType: ModifyRequestType
    ): Boolean {
        val script: Script = try {
            @Suppress("NAME_SHADOWING")
            val stringScript = "@preset ${ScriptUtils.toString(presetName)} $stringScript"
            scriptProcessor.parse(
                System.currentTimeMillis(),
                player.uniqueId,
                stringScript
            )
        } catch (e: ParseException) {
            player.sendMessage(Prefix.ERROR + "Failed to parse script. ${e.message}")
            return true
        }

        request(player, script, modifyRequestType)

        return true
    }

    fun request(player: Player, script: Script, modifyRequestType: ModifyRequestType) {
        player.sendMessage(Prefix.INFO + "Please click any block...")
        requests.putRequest(player, modifyRequestType.toRequest(script))
    }
}

private sealed class ModifyRequestType {
    object Add : ModifyRequestType() {
        override fun toRequest(script: Script): Request {
            return Request.Add(script)
        }
    }

    object Embed : ModifyRequestType() {
        override fun toRequest(script: Script): Request {
            return Request.Embed(script)
        }
    }

    abstract fun toRequest(script: Script): Request
}
