package com.github.kuro46.embedscript

import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.plugin.java.JavaPlugin

import java.io.IOException
import java.util.logging.Level

/**
 * @author shirokuro
 */
class EmbedScriptPlugin : JavaPlugin() {
    override fun onEnable() {
        val begin = System.currentTimeMillis()
        try {
            EmbedScript.initialize(this)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Failed to initialize the plugin! disabling...", e)
            Bukkit.getPluginManager().disablePlugin(this)
            return
        } catch (e: InvalidConfigurationException) {
            logger.log(Level.SEVERE, "Failed to initialize the plugin! disabling...", e)
            Bukkit.getPluginManager().disablePlugin(this)
            return
        } catch (e: IllegalStateException) {
            logger.log(Level.SEVERE, "Failed to initialize the plugin! disabling...", e)
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        val end = System.currentTimeMillis()
        logger.info("Enabled! (${end - begin}ms)")
    }

    override fun onDisable() {
        EmbedScript.reset()
    }
}
