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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class EmbedScript {
    private static volatile EmbedScript instance;

    private final Plugin plugin;
    private final Path dataFolder;
    private final Logger logger;
    private final ScriptManager scriptManager;
    private final Configuration configuration;
    private final ScriptUI scriptUI;
    private final Requests requests;
    private final ScriptParser scriptParser;

    public static synchronized void initialize(Plugin plugin)
        throws IOException, InvalidConfigurationException, IllegalStateException {

        if (instance != null) {
            throw new IllegalStateException("Instance already initialized!");
        }

        instance = new EmbedScript(plugin);
    }

    public static synchronized void reset() {
        instance = null;
    }

    private EmbedScript(Plugin plugin) throws IOException, InvalidConfigurationException {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath();
        this.logger = plugin.getLogger();

        this.scriptManager = loadScripts();

        this.configuration = loadConfiguration();

        this.scriptUI = new ScriptUI(scriptManager);
        this.requests = new Requests(scriptUI);
        this.scriptParser = new ScriptParser(configuration);

        registerCommands();
        registerListeners();
    }

    private Configuration loadConfiguration() throws IOException, InvalidConfigurationException {
        plugin.saveDefaultConfig();
        return Configuration.load(dataFolder);
    }

    private ScriptManager loadScripts() throws IOException {
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }

        Path filePath = dataFolder.resolve("scripts.json");

        migrateFromOldFormatIfNeeded(filePath);

        return ScriptManager.load(filePath);
    }

    private void migrateFromOldFormatIfNeeded(Path scriptFilePath) throws IOException {
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

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new InteractListener(this), plugin);
        pluginManager.registerEvents(new MoveListener(this), plugin);
    }

    private void registerCommands() {
        for (EventType eventType : EventType.values()) {
            Bukkit.getPluginCommand(eventType.getCommandName())
                .setExecutor(new ESCommandExecutor(this, eventType.getPresetName()));
        }
        Bukkit.getPluginCommand("embedscript").setExecutor(new ESCommandExecutor(this));
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public Logger getLogger() {
        return logger;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ScriptUI getScriptUI() {
        return scriptUI;
    }

    public Requests getRequests() {
        return requests;
    }

    public ScriptParser getScriptParser() {
        return scriptParser;
    }
}
