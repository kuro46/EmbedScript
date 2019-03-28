package com.github.kuro46.embedscript.request

import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUI
import org.bukkit.entity.Player
import java.util.*

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
        val request = removeRequest(player) ?: return false

        when (request.requestType) {
            RequestType.VIEW -> scriptUI.view(player, position)
            RequestType.EMBED -> scriptUI.embed(player, position, request.script!!)
            RequestType.ADD -> scriptUI.add(player, position, request.script!!)
            RequestType.REMOVE -> scriptUI.remove(player, position)
        }
        return true
    }
}
