package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.command.EventCommandExecutor;
import com.github.kuro46.embedscript.command.MainCommandExecutor;
import com.github.kuro46.embedscript.listener.InteractListener;
import com.github.kuro46.embedscript.listener.MoveListener;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

/**
 * @author shirokuro
 */
public class EmbedScriptPlugin extends JavaPlugin implements Listener {
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        try {
            scriptManager = new ScriptManager(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Requests requests = new Requests(this, scriptManager);
        for (EventType eventType : EventType.values()) {
            getCommand(eventType.getCommandName())
                .setExecutor(new EventCommandExecutor(eventType, requests, scriptManager));
        }
        getCommand("embedscript").setExecutor(new MainCommandExecutor());
        CommandPerformer commandPerformer = new CommandPerformer(this);
        new InteractListener(this, scriptManager, requests, commandPerformer);
        new MoveListener(this, scriptManager, commandPerformer);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin plugin = event.getPlugin();
        if (plugin.getName().equals("ScriptBlock")) {
            ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
            consoleSender.sendMessage(Prefix.PREFIX + "ScriptBlock found! Migrating scripts.");
            try {
                new Migrator(consoleSender, scriptManager, plugin);
            } catch (Exception e) {
                Bukkit.getPluginManager().disablePlugin(this);
                throw new RuntimeException("Failed to migration! Disabling EmbedScript.", e);
            }
            consoleSender.sendMessage(Prefix.SUCCESS_PREFIX + "Scripts has been migrated. Disabling ScriptBlock.");

            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }
}