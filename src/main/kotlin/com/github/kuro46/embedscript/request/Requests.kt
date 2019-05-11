package com.github.kuro46.embedscript.request

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptPosition
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.WeakHashMap

/**
 * @author shirokuro
 */
class Requests(private val scriptManager: ScriptManager) {
    private val requests = WeakHashMap<Player, Request>(2)

    fun putRequest(player: Player, request: Request): Request? {
        return requests.put(player, request)
    }

    fun removeRequest(player: Player): Request? {
        return requests.remove(player)
    }

    fun hasRequest(player: Player): Boolean {
        return requests.containsKey(player)
    }

    fun executeRequest(player: Player, position: ScriptPosition): Boolean {
        when (val request = removeRequest(player) ?: return false) {
            is Request.View -> {
                player.performCommand("embedscript view ${position.world} ${position.x} ${position.y} ${position.z}")
            }
            is Request.Remove -> remove(player, position)
            is Request.Embed -> embed(player, position, request.script)
            is Request.Add -> add(player, position, request.script)
        }
        return true
    }

    fun embed(sender: CommandSender,
              position: ScriptPosition,
              script: Script) {
        if (scriptManager.contains(position)) {
            sender.sendMessage(Prefix.ERROR + "Script already exists in that place.")
            return
        }

        scriptManager.add(position, script)

        sender.sendMessage(Prefix.SUCCESS + "Script was successfully embedded.")
    }

    fun add(sender: CommandSender,
            position: ScriptPosition,
            script: Script) {
        if (!scriptManager.contains(position)) {
            sender.sendMessage(Prefix.ERROR + "Script not exists in that place.")
            return
        }
        scriptManager.add(position, script)

        sender.sendMessage(Prefix.SUCCESS + "Script was successfully added.")
    }

    fun remove(sender: CommandSender, position: ScriptPosition) {
        if (scriptManager.remove(position) == null) {
            sender.sendMessage(Prefix.ERROR + "Script not exists in that place.")
            return
        }

        sender.sendMessage(Prefix.SUCCESS + "Script was successfully removed.")
    }
}
