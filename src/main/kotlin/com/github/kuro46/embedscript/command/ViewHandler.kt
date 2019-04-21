package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.request.Requests
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUI
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.util.MojangUtil
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.UUID
import java.util.stream.Collectors
import kotlin.streams.toList

class ViewHandler(private val requests: Requests,
                  private val scriptManager: ScriptManager,
                  private val scriptUI: ScriptUI) : CommandHandler() {
    override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
        return when {
            // pass to InteractListener
            args.isEmpty() -> {
                passToInteractListener(sender)
                true
            }
            // display script information to player!
            args.isElementEnough(3) -> {
                displayScriptInformation(args, sender)
                true
            }
            // We don't know
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
        return if (uncompletedArgIndex == 0) {
            // wants suggest worlds
            Bukkit.getWorlds().stream().map { it.name }.toList()
        } else {
            emptyList()
        }
    }

    private fun passToInteractListener(sender: CommandSender) {
        val player = CommandHandlerUtil.castToPlayer(sender) ?: return
        player.sendMessage(Prefix.PREFIX + "Please click any block...")
        requests.putRequest(player, Request.View)
    }

    private fun displayScriptInformation(args: Arguments, sender: CommandSender) {
        val world = args[0]
        val x = args.getInt(sender, 1) ?: return
        val y = args.getInt(sender, 2) ?: return
        val z = args.getInt(sender, 3) ?: return
        val pageNumber = args.getInt(sender, 4, 1) ?: return

        val position = ScriptPosition(world, x, y, z)
        if (!scriptManager.contains(position)) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.")
            return
        }
        val messages: MutableList<Array<BaseComponent>> = ArrayList()
        for (script in scriptManager[position]) {
            // null if failed to find name of author
            val author = getUserName(sender, script.author) ?: return
            val formatted = formatTime(script.createdAt)
            messages.add(TextComponent.fromLegacyText("$author created at $formatted"))
            // TODO: delete none
            messages.add(TextComponent.fromLegacyText("@listen-move " + collectionToString(script.moveTypes)))
            messages.add(TextComponent.fromLegacyText("@listen-click " + collectionToString(script.clickTypes)))
            messages.add(TextComponent.fromLegacyText("@listen-push " + collectionToString(script.pushTypes)))
            val scriptMap = script.script
            for (key in scriptMap.keySet()) {
                val value = scriptMap.get(key)
                messages.add(TextComponent.fromLegacyText("@$key ${ScriptUtil.toString(value)}"))
            }
        }
        scriptUI.sendPage("Script information", sender, messages, pageNumber - 1, 12) { index ->
            val pageNum = index + 1
            "/embedscript view $world $x $y $z $pageNum"
        }
    }

    private fun getUserName(sender: CommandSender, uuid: UUID): String? {
        return Bukkit.getPlayer(uuid)?.name ?: run {
            val result = MojangUtil.getName(uuid)
            if (result == null) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to find user name")
            }
            result
        }
    }

    private fun formatTime(time: Long): String {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
        return dateTimeFormatter.format(Instant.ofEpochMilli(time))
    }

    private fun collectionToString(collection: Collection<*>): String {
        return when {
            collection.isEmpty() -> "NONE"
            collection.size == 1 -> ScriptUtil.toString(collection.iterator().next().toString())
            else -> collection.stream()
                    .map { it.toString() }
                    .map { s -> s + ChatColor.RESET }
                    .collect(Collectors.joining("][", "[", "]"))
        }
    }
}
