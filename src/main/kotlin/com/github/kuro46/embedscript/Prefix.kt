package com.github.kuro46.embedscript

import org.bukkit.ChatColor

/**
 * @author shirokuro
 */
class Prefix private constructor() {

    init {
        throw UnsupportedOperationException("Constant class.")
    }

    companion object {
        val PREFIX = (ChatColor.GRAY.toString() + "<"
                + ChatColor.DARK_GREEN + "E" + ChatColor.GREEN + "S" + ChatColor.GRAY + "> ")
        val WARN_PREFIX = PREFIX + ChatColor.GOLD
        val ERROR_PREFIX = PREFIX + ChatColor.RED
        val SUCCESS_PREFIX = PREFIX + ChatColor.GREEN
    }
}
