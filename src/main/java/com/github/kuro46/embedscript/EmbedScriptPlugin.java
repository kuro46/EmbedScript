package com.github.kuro46.embedscript;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author shirokuro
 */
public class EmbedScriptPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        long begin = System.currentTimeMillis();
        try {
            EmbedScript.initialize(this);
        } catch (IOException | InvalidConfigurationException | IllegalStateException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize the plugin! disabling...", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        long end = System.currentTimeMillis();
        getLogger().info(String.format("Enabled! (%sms)", end - begin));
    }

    @Override
    public void onDisable() {
        EmbedScript.reset();
    }
}
