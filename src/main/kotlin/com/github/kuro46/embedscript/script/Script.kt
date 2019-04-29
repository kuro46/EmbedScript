package com.github.kuro46.embedscript.script

import com.google.common.collect.ImmutableListMultimap
import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * @author shirokuro
 */
class Script(val author: UUID,
             val createdAt: Long,
             val moveTypes: Set<MoveType>,
             val clickTypes: Set<ClickType>,
             val pushTypes: Set<PushType>,
             val script: ImmutableListMultimap<String, String>) {

    enum class MoveType {
        ALL,
        GROUND
    }

    enum class ClickType {
        ALL,
        RIGHT,
        LEFT;


        companion object {

            fun getByAction(action: Action): ClickType? {
                return if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    RIGHT
                } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    LEFT
                } else {
                    null
                }
            }
        }
    }

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
                        clickedBlockType == Material.WOOD_PLATE) {
                    PLATE
                } else {
                    null
                }
            }
        }
    }
}
