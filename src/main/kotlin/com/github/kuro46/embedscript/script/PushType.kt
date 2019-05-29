package com.github.kuro46.embedscript.script

import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

enum class PushType {
    ALL,
    BUTTON,
    PLATE;

    companion object {

        fun getByEvent(event: PlayerInteractEvent): PushType? {
            if (event.action != Action.PHYSICAL) {
                return null
            }

            val clickedBlockType = event.clickedBlock.type
            return if (clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.WOOD_BUTTON) {
                BUTTON
            } else if (clickedBlockType == Material.GOLD_PLATE ||
                clickedBlockType == Material.IRON_PLATE ||
                clickedBlockType == Material.STONE_PLATE ||
                clickedBlockType == Material.WOOD_PLATE
            ) {
                PLATE
            } else {
                null
            }
        }
    }
}
