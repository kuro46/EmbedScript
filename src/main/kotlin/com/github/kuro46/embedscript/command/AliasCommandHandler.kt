package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.EventType
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptUtils
import com.github.kuro46.embedscript.script.executor.ScriptProcessor
import com.github.kuro46.embedscript.util.command.Arguments
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.RootCommandHandler
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * CommandHandler for alias command (/esinteract, /eswalk)
 *
 * @author shirokuro
 */
class AliasCommandHandler(
    eventType: EventType,
    scriptProcessor: ScriptProcessor,
    requests: Requests
) : RootCommandHandler() {
    init {
        val scriptRequester = ScriptRequester(eventType.presetName, scriptProcessor, requests)

        registerChildHandler("help", HelpHandler(eventType))
        registerChildHandler("add", ModifyCommandHandler(ModifyRequestType.Add, scriptRequester))
        registerChildHandler("embed", ModifyCommandHandler(ModifyRequestType.Embed, scriptRequester))
        registerChildHandler("sbAdd", SBModifyCommandHandler(
            scriptRequester,
            ModifyRequestType.Add,
            eventType,
            scriptProcessor
        ))
        registerChildHandler("sbCreate", SBModifyCommandHandler(
            scriptRequester,
            ModifyRequestType.Embed,
            eventType,
            scriptProcessor
        ))
    }

    override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        return false
    }
}

private class HelpHandler(private val eventType: EventType) : CommandHandler() {
    override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        @Suppress("NAME_SHADOWING")
        val command = "/${eventType.commandName}"
        sender.sendMessage("""
            $command help - Displays this message.
            $command add - Adds a script to the clicked block. (prefixed with '@preset [${eventType.presetName}]')
            $command embed - Embeds a script to the clicked block. (prefixed with '@preset [${eventType.presetName}]')
            $command sbAdd - Add a script to the clicked block. (similar to '/sb${eventType.name.toLowerCase()} add')
            $command sbCreate - Embeds a script to the clicked block. (similar to '/sb${eventType.name.toLowerCase()} create')
        """.trimIndent())

        return true
    }
}

private class SBModifyCommandHandler(
    private val scriptRequester: ScriptRequester,
    private val requestType: ModifyRequestType,
    private val eventType: EventType,
    private val processor: ScriptProcessor
) : CommandHandler(SenderType.Player()) {
    override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        @Suppress("NAME_SHADOWING")
        val sender = sender as Player
        val script = try {
            ScriptUtils.createScriptFromLegacyFormat(
                processor,
                sender.uniqueId,
                eventType,
                args.joinToString(" ")
            )
        } catch (e: ParseException) {
            sender.sendMessage(Prefix.ERROR + "Failed to parse the script: ${e.message}")
            return true
        }
        scriptRequester.request(sender, script, requestType)

        return true
    }
}

private class ModifyCommandHandler(
    private val requestType: ModifyRequestType,
    private val scriptRequester: ScriptRequester
) : CommandHandler(SenderType.Player()) {
    override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        scriptRequester.request(sender as Player, args, requestType)
        return true
    }
}

private class ScriptRequester(
    private val presetName: String,
    private val scriptProcessor: ScriptProcessor,
    private val requests: Requests
) {
    fun request(player: Player, args: Arguments, modifyRequestType: ModifyRequestType): Boolean {
        if (args.isEmpty()) {
            return false
        }
        val script: Script = try {
            val stringScript = "@preset ${ScriptUtils.toString(presetName)} ${args.joinToString(" ")}"
            scriptProcessor.parse(System.currentTimeMillis(), player.uniqueId, stringScript)
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
