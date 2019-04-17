package com.github.kuro46.embedscript.request

import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUI
import org.bukkit.entity.Player
import java.util.WeakHashMap

/**
 * @author shirokuro
 */
class Requests(private val scriptUI: ScriptUI) {
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
            is Request.View-> scriptUI.view(player, position)
            is Request.Remove -> scriptUI.remove(player, position)
            is Request.Embed -> scriptUI.embed(player, position, request.script)
            is Request.Add -> scriptUI.add(player, position, request.script)
        }
        return true
    }
}
