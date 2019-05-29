package com.github.kuro46.embedscript.listener

import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.script.ClickType
import com.github.kuro46.embedscript.script.PushType
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.executor.ScriptExecutor
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * @author shirokuro
 */
class InteractListener(embedScript: EmbedScript) : Listener {
    companion object {
        private const val EXECUTION_INTERVAL = 300L
    }

    private val coolTime: Cache<UUID, Boolean> = CacheBuilder.newBuilder()
        .expireAfterWrite(EXECUTION_INTERVAL, TimeUnit.MILLISECONDS)
        .build()
    private val scriptManager = embedScript.scriptManager
    private val scriptExecutor: ScriptExecutor = embedScript.scriptExecutor
    private val requests = embedScript.requests

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (!event.hasBlock() || isInCoolTime(player)) {
            return
        }
        updateCoolTime(player)

        val position = ScriptPosition(event.clickedBlock)
        if (event.action == Action.RIGHT_CLICK_BLOCK &&
            requests.executeRequest(player, position)
        ) {
            return
        }

        val scriptList = scriptManager[position] ?: return

        val filteredScriptList = scriptList.filter {
            validateClickType(it, event.action) || validatePushType(it, event)
        }

        if (filteredScriptList.isNotEmpty()) {
            scriptExecutor.execute(player, filteredScriptList, position)
            event.isCancelled = true
        }
    }

    private fun validateClickType(script: Script, action: Action): Boolean {
        val clickTypes = script.clickTypes
        if (clickTypes.isEmpty()) {
            return false
        }

        val clickTypeOfEvent = ClickType.getByAction(action) ?: return false
        // PHYSICAL action

        for (clickType in clickTypes) {
            if (clickType == ClickType.ALL || clickType == clickTypeOfEvent) {
                return true
            }
        }

        return false
    }

    private fun validatePushType(script: Script, event: PlayerInteractEvent): Boolean {
        val pushTypes = script.pushTypes
        if (pushTypes.isEmpty()) {
            return false
        }

        val pushTypeOfEvent = PushType.getByEvent(event) ?: return false
        //Not PHYSICAL or Unknown material

        for (pushType in pushTypes) {
            if (pushType == PushType.ALL || pushType == pushTypeOfEvent) {
                return true
            }
        }

        return false
    }

    private fun isInCoolTime(player: Player): Boolean {
        return coolTime.getIfPresent(player.uniqueId) != null
    }

    private fun updateCoolTime(player: Player) {
        coolTime.put(player.uniqueId, true)
    }
}
