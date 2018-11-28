package com.github.kuro46.embedscript.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * @author shirokuro
 */
public class AbstractListener implements Listener {
    public AbstractListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
