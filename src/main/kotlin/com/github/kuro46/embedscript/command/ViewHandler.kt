package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.ExecutionThreadType
import com.github.kuro46.embedscript.util.command.LastArgument

/**
 * @author shirokuro
 */
class ViewHandler(
    private val requests: Requests,
    private val scriptManager: ScriptManager
) : CommandHandler(
    ExecutionThreadType.ASYNCHRONOUS,
    ArgumentInfoList(
        emptyList(),
        LastArgument.NotAllow
    ),
    "Displays information about scripts in the clicked block."
) {
    override fun handleCommand(
        senderHolder: CommandSenderHolder,
        args: Map<String, String>
    ) {
        val player = senderHolder.tryCastToPlayerOrMessage() ?: return
        player.sendMessage(Prefix.INFO + "Please click any block...")
        requests.putRequest(player, Request.View)
    }
}
