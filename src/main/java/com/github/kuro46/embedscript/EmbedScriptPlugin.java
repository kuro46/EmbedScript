package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.command.EventCommandExecutor;
import com.github.kuro46.embedscript.command.MainCommandExecutor;
import com.github.kuro46.embedscript.listener.InteractListener;
import com.github.kuro46.embedscript.listener.MoveListener;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptUI;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author shirokuro
 */
public class EmbedScriptPlugin extends JavaPlugin implements Listener {
    private ScriptUI scriptUI = new ScriptUI();

    @Override
    public void onEnable() {
        try {
            ScriptManager.load(getDataFolder().toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Requests requests = new Requests(scriptUI);
        registerCommands(requests);

        registerListeners(requests);
    }

    private void registerListeners(Requests requests) {
    PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(
            new InteractListener(this,ScriptManager.get(EventType.INTERACT), requests), this);
        pluginManager.registerEvents(
            new MoveListener(this,ScriptManager.get(EventType.WALK)), this);
        pluginManager.registerEvents(this, this);
    }

    private void registerCommands(Requests requests) {
        for (EventType eventType : EventType.values()) {
            getCommand(eventType.getCommandName())
                .setExecutor(new EventCommandExecutor(eventType, requests, scriptUI));
        }
        getCommand("embedscript").setExecutor(new MainCommandExecutor());
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin plugin = event.getPlugin();
        if (plugin.getName().equals("ScriptBlock")) {
            ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
            consoleSender.sendMessage(Prefix.PREFIX + "ScriptBlock found! Migrating scripts.");
            try {
                Migrator.migrate(consoleSender, scriptUI, plugin);
            } catch (Exception e) {
                Bukkit.getPluginManager().disablePlugin(this);
                throw new RuntimeException("Failed to migration! Disabling EmbedScript.", e);
            }
            consoleSender.sendMessage(Prefix.SUCCESS_PREFIX + "Scripts has been migrated. Disabling ScriptBlock.");

            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }
}
