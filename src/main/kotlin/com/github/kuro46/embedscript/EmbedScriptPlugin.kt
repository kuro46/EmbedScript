package com.github.kuro46.embedscript

import java.util.logging.Level
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author shirokuro
 */
class EmbedScriptPlugin : JavaPlugin() {
    override fun onEnable() {
        val begin = System.currentTimeMillis()

        val initResult = runCatching { EmbedScript.initialize(this) }
        initResult.exceptionOrNull()?.let {
            logger.log(Level.SEVERE, "Failed to initialize the plugin! disabling...", it)
            Bukkit.getPluginManager().disablePlugin(this)
        } ?: run {
            val end = System.currentTimeMillis()
            logger.info("Enabled! (${end - begin}ms)")
        }
    }

    override fun onDisable() {
        EmbedScript.reset()
    }
}
