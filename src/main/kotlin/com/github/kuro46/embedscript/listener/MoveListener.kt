package com.github.kuro46.embedscript.listener

import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.script.MoveType
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

/**
 * @author shirokuro
 */
class MoveListener(embedScript: EmbedScript) : Listener {
    companion object {
        private const val GROUND_ACCEPTABLE_MAXIMUM_RANGE = 0.2
    }

    private val scriptProcessor = embedScript.scriptProcessor
    private val scriptManager = embedScript.scriptManager

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val to = event.to
        if (equalsBlock(event.from, to)) {
            return
        }

        val player = event.player
        val scriptPosition = ScriptPosition(
            to.world.name,
            to.blockX,
            to.blockY - 1,
            to.blockZ
        )

        val scriptList = scriptManager[scriptPosition] ?: return

        val filteredScriptList = scriptList.filter { validateMoveType(it, event) }
        if (filteredScriptList.isNotEmpty()) {
            scriptProcessor.execute(player, filteredScriptList, scriptPosition)
        }
    }

    private fun validateMoveType(script: Script, event: PlayerMoveEvent): Boolean {
        val moveTypes = script.moveTypes
        if (moveTypes.isEmpty()) {
            return false
        }

        for (moveType in moveTypes) {
            if (moveType == MoveType.ALL) {
                return true
            } else if (moveType == MoveType.GROUND && validateGroundMoveType(event)) {
                return true
            }
        }

        return false
    }

    private fun validateGroundMoveType(event: PlayerMoveEvent): Boolean {
        val to = event.to
        //Expects air
        val upperSurface = to.block
        //Expects non-air
        val downerSurface = upperSurface.getRelative(BlockFace.DOWN)

        return upperSurface.type == Material.AIR &&
            downerSurface.type != Material.AIR &&
            to.y - upperSurface.y <= GROUND_ACCEPTABLE_MAXIMUM_RANGE
    }

    private fun equalsBlock(location: Location, location1: Location): Boolean {
        return location.blockX == location1.blockX &&
            location.blockY == location1.blockY &&
            location.blockZ == location1.blockZ
    }
}
