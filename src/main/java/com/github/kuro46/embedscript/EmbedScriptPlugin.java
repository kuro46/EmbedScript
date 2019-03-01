package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.listener.InteractListener;
import com.github.kuro46.embedscript.listener.MoveListener;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.ScriptSerializer;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.script.parser.ScriptParser;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shirokuro
 */
public class EmbedScriptPlugin extends JavaPlugin implements Listener {
    private ScriptManager scriptManager;
    private ScriptUI scriptUI;

    @Override
    public void onEnable() {
        long begin = System.currentTimeMillis();

        try {
            Path dataFolder = getDataFolder().toPath();
            if (Files.notExists(dataFolder)) {
                Files.createDirectory(dataFolder);
            }

            Path filePath = dataFolder.resolve("scripts.json");

            migrateFromOldFormatIfNeeded(filePath, dataFolder);

            scriptManager = ScriptManager.load(filePath);
            scriptUI = new ScriptUI(scriptManager);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Requests requests = new Requests(scriptUI);
        ScriptParser scriptParser = new ScriptParser();
        registerCommands(requests, scriptParser);

        registerListeners(requests);

        long end = System.currentTimeMillis();
        getLogger().info(String.format("Enabled! (%sms)", end - begin));
    }

    private void registerListeners(Requests requests) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new InteractListener(this, scriptManager, requests), this);
        pluginManager.registerEvents(new MoveListener(this, scriptManager), this);
        pluginManager.registerEvents(this, this);
    }

    private void registerCommands(Requests requests, ScriptParser scriptParser) {
        for (EventType eventType : EventType.values()) {
            getCommand(eventType.getCommandName())
                .setExecutor(new ESCommandExecutor(scriptParser, eventType.getPreset(), scriptUI, requests));
        }
        getCommand("embedscript").setExecutor(new ESCommandExecutor(scriptParser, scriptUI, requests));
    }

    private void migrateFromOldFormatIfNeeded(Path scriptFilePath, Path dataFolder) throws IOException {
        if (Files.exists(scriptFilePath)) {
            return;
        }

        Map<ScriptPosition, List<Script>> merged = new HashMap<>();
        try {
            Arrays.stream(EventType.values())
                .map(eventType -> dataFolder.resolve(eventType.getFileName()))
                .filter(path -> Files.exists(path))
                .map(path -> {
                    try {
                        return ScriptManager.load(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .forEach(scriptManager -> {
                    for (Map.Entry<ScriptPosition, List<Script>> entry : scriptManager.entrySet()) {
                        ScriptPosition position = entry.getKey();
                        List<Script> scripts = entry.getValue();
                        List<Script> mergeTo = merged.computeIfAbsent(position, ignore -> new ArrayList<>());

                        mergeTo.addAll(scripts);
                    }

                    try {
                        Files.delete(scriptManager.getPath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        if (merged.isEmpty()) {
            return;
        }

        ScriptSerializer.serialize(scriptFilePath, merged);
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
