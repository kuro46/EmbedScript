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
import org.bukkit.configuration.InvalidConfigurationException;
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
import java.util.logging.Level;

/**
 * @author shirokuro
 */
public class EmbedScriptPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        long begin = System.currentTimeMillis();

        ScriptManager scriptManager;

        try {
            scriptManager = loadScripts();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load the scripts! disabling plugin...", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Configuration configuration;
        try {
            configuration = loadConfiguration();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().log(Level.SEVERE, "Failed to load the configuration! disabling plugin...", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ScriptUI scriptUI = new ScriptUI(scriptManager);
        Requests requests = new Requests(scriptUI);
        ScriptParser scriptParser = new ScriptParser(configuration);

        registerCommands(requests, scriptParser, scriptUI);
        registerListeners(requests, scriptManager, scriptUI);

        long end = System.currentTimeMillis();
        getLogger().info(String.format("Enabled! (%sms)", end - begin));
    }

    private Configuration loadConfiguration() throws IOException, InvalidConfigurationException {
        saveDefaultConfig();
        return new Configuration(getDataFolder().toPath().resolve("config.yml"));
    }

    private ScriptManager loadScripts() throws IOException {
        Path dataFolder = getDataFolder().toPath();
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }

        Path filePath = dataFolder.resolve("scripts.json");

        migrateFromOldFormatIfNeeded(filePath, dataFolder);

        return ScriptManager.load(filePath);
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

    private void registerListeners(Requests requests, ScriptManager scriptManager, ScriptUI scriptUI) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new InteractListener(this, scriptManager, requests), this);
        pluginManager.registerEvents(new MoveListener(this, scriptManager), this);
        pluginManager.registerEvents(new PluginEnableListener(this, scriptUI), this);
    }

    private void registerCommands(Requests requests, ScriptParser scriptParser, ScriptUI scriptUI) {
        for (EventType eventType : EventType.values()) {
            getCommand(eventType.getCommandName())
                .setExecutor(new ESCommandExecutor(scriptParser, eventType.getPresetName(), scriptUI, requests));
        }
        getCommand("embedscript").setExecutor(new ESCommandExecutor(scriptParser, scriptUI, requests));
    }

    private static class PluginEnableListener implements Listener {
        private final Plugin embedScript;
        private final ScriptUI scriptUI;

        PluginEnableListener(Plugin embedScript, ScriptUI scriptUI) {
            this.embedScript = embedScript;
            this.scriptUI = scriptUI;
        }

        @EventHandler
        public void onPluginEnable(PluginEnableEvent event) {
            Plugin plugin = event.getPlugin();
            if (plugin.getName().equals("ScriptBlock")) {
                ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
                consoleSender.sendMessage(Prefix.PREFIX + "ScriptBlock found! Migrating scripts.");
                try {
                    Migrator.migrate(consoleSender, scriptUI, plugin);
                } catch (Exception e) {
                    Bukkit.getPluginManager().disablePlugin(this.embedScript);
                    throw new RuntimeException("Failed to migration! Disabling EmbedScript.", e);
                }
                consoleSender.sendMessage(Prefix.SUCCESS_PREFIX + "Scripts has been migrated. Disabling ScriptBlock.");

                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }
    }
}
