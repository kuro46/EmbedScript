package com.github.kuro46.embedscript;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    private final Path configPath;
    private Map<String,String> presets;
    private Map<String, List<String>> permissionsForActions;
    private int parseLoopLimit;

    private Configuration(Path dataFolder) throws IOException, InvalidConfigurationException {
        this.configPath = dataFolder.resolve("config.yml");
        load();
    }

    public static Configuration load(Path dataFolder) throws IOException, InvalidConfigurationException {
        return new Configuration(dataFolder);
    }

    public void load() throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            configuration.load(reader);
        }

        // load parse-loop-limit
        parseLoopLimit = configuration.getInt("parse-loop-limit", 3);

        loadPresets(configuration);

        loadPermissionsForActions(configuration);
    }

    private void loadPresets(org.bukkit.configuration.Configuration configuration) {
        Map<String, String> presets = new HashMap<>();
        ConfigurationSection presetsSection = configuration.getConfigurationSection("presets");
        for (String presetName : presetsSection.getKeys(false)) {
            String presetValue = presetsSection.getString(presetName);
            presets.put(presetName, presetValue);
        }
        this.presets = Collections.unmodifiableMap(presets);
    }

    private void loadPermissionsForActions(FileConfiguration configuration) throws IOException {
        // load
        Map<String, List<String>> permissionsForActions = new HashMap<>();
        ConfigurationSection permForActionsSection
            = configuration.getConfigurationSection("permissions-for-actions");
        if (permForActionsSection != null) {
            for (String action : permForActionsSection.getKeys(false)) {
                List<String> permissions = permForActionsSection.getStringList(action);
                permissionsForActions.put(action, permissions);
            }
        }
        // update
        boolean needSave = false;
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();
            if (commands == null) {
                continue;
            }
            for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                String command = entry.getKey();
                String permission = (String) entry.getValue().get("permission");

                if (permissionsForActions.containsKey(command)) {
                    continue;
                }

                List<String> permissions = permission == null
                    // note:
                    // please don't replace with Collections.emptyList()
                    // because SnakeYaml cannot serialize Collections.emptyList()
                    ? new ArrayList<>(0)
                    : Collections.singletonList(permission);

                permissionsForActions.put(command, permissions);
                needSave = true;
            }
        }
        // save if needed
        if (needSave) {
            configuration.set("permissions-for-actions", permissionsForActions);
            configuration.save(configPath.toFile());
        }

        this.permissionsForActions = Collections.unmodifiableMap(permissionsForActions);
    }

    public Map<String, String> getPresets() {
        return presets;
    }

    public Map<String, List<String>> getPermissionsForActions() {
        return permissionsForActions;
    }

    public int getParseLoopLimit() {
        return parseLoopLimit;
    }
}
