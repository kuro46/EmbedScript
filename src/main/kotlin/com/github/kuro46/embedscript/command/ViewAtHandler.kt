package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtils
import com.github.kuro46.embedscript.util.MojangUtils
import com.github.kuro46.embedscript.util.PageUtils
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.ExecutionThreadType
import com.github.kuro46.embedscript.util.command.OptionalArgumentInfo
import com.github.kuro46.embedscript.util.command.OptionalArguments
import com.github.kuro46.embedscript.util.command.RequiedArgumentInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.UUID
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

class ViewAtHandler(
    val scriptManager: ScriptManager
) : CommandHandler(
    ExecutionThreadType.ASYNCHRONOUS,
    ArgumentInfoList(
        listOf(
            RequiedArgumentInfo("world"),
            RequiedArgumentInfo("x"),
            RequiedArgumentInfo("y"),
            RequiedArgumentInfo("z")
        ),
        OptionalArguments(listOf(OptionalArgumentInfo("pageNumber", "1")))
    ),
    "Displays information about scripts in the specified coordinate."
) {
    override fun handleCommand(
        senderHolder: CommandSenderHolder,
        args: Map<String, String>
    ) {
        val sender = senderHolder.commandSender

        val world = args.getValue("world")
        val x = args.getValue("x").toInt()
        val y = args.getValue("y").toInt()
        val z = args.getValue("z").toInt()
        val pageNumber = args.getValue("pageNumber").toInt()
        val position = ScriptPosition(world, x, y, z)
        val scripts = scriptManager.getScripts()
        if (!scripts.contains(position)) {
            sender.sendMessage(Prefix.ERROR + "Script not exists in that place.")
            return
        }
        val messages: MutableList<Array<BaseComponent>> = ArrayList()
        for (script in scripts.getValue(position)) {
            // null if failed to find name of author
            val author = getUserName(sender, script.author) ?: return
            val timestamp = if (script.createdAt != -1L) {
                "at ${formatTime(script.createdAt)}"
            } else {
                ""
            }
            messages.add(TextComponent.fromLegacyText("$author created $timestamp"))
            addMessageIfNeeded(messages, "@listen-move", script.moveTypes)
            addMessageIfNeeded(messages, "@listen-click", script.clickTypes)
            addMessageIfNeeded(messages, "@listen-push", script.pushTypes)
            for (parent in script.keys) {
                for ((key, values) in parent) {
                    messages.add(
                        TextComponent.fromLegacyText(
                            "@$key ${ScriptUtils.toString(values)}"
                        )
                    )
                }
            }
        }

        PageUtils.sendPage(
            "Script information",
            sender,
            messages,
            pageNumber - 1,
            12
        ) { index ->
            val pageNum = index + 1
            "/embedscript viewAt $world $x $y $z $pageNum"
        }
    }

    private fun addMessageIfNeeded(
        addTo: MutableList<Array<BaseComponent>>,
        key: String,
        values: Collection<*>
    ) {
        if (values.isEmpty()) {
            return
        }

        val string = ScriptUtils.toString(values.map { it.toString() + ChatColor.RESET })
        addTo.add(TextComponent.fromLegacyText("$key $string"))
    }

    private fun getUserName(sender: CommandSender, uuid: UUID): String? {
        return Bukkit.getPlayer(uuid)?.name ?: run {
            val result = MojangUtils.getName(uuid)
            if (result == null) {
                sender.sendMessage(Prefix.ERROR + "Failed to find user name")
            }
            result
        }
    }

    private fun formatTime(time: Long): String {
        val dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
        return dateTimeFormatter.format(Instant.ofEpochMilli(time))
    }

    override fun handleTabComplete(
        senderHolder: CommandSenderHolder,
        commandName: String,
        completedArgs: List<String>,
        uncompletedArg: String
    ): List<String> {
        return if (completedArgs.isEmpty()) {
            Bukkit.getWorlds().map { it.name }
        } else {
            emptyList()
        }
    }
}
