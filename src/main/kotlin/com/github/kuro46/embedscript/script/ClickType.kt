package com.github.kuro46.embedscript.script;

import org.bukkit.event.block.Action

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
