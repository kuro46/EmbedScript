package com.github.kuro46.embedscript

import org.bukkit.ChatColor

/**
 * @author shirokuro
 */
object Prefix {
    val PREFIX = (ChatColor.GRAY.toString() + "<"
            + ChatColor.DARK_GREEN + "E" + ChatColor.GREEN + "S" + ChatColor.GRAY + "> ")
    val WARN_PREFIX = PREFIX + ChatColor.GOLD
    val ERROR_PREFIX = PREFIX + ChatColor.RED
    val SUCCESS_PREFIX = PREFIX + ChatColor.GREEN
}
