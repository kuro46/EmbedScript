package com.github.kuro46.embedscript;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
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

    public Configuration(Path configPath) throws IOException, InvalidConfigurationException {
        this.configPath = configPath;
        load();
    }

    public void load() throws IOException, InvalidConfigurationException {
        presets = new HashMap<>();
        permissionsForActions = new HashMap<>();

        YamlConfiguration configuration = new YamlConfiguration();
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            configuration.load(reader);
        }

        // load parse-loop-limit
        parseLoopLimit = configuration.getInt("parse-loop-limit", 3);

        // load presets
        ConfigurationSection presetsSection = configuration.getConfigurationSection("presets");
        for (String presetName : presetsSection.getKeys(false)) {
            String presetValue = presetsSection.getString(presetName);
            presets.put(presetName,presetValue);
        }

        // load permissions for commands
        ConfigurationSection permForActionsSection
            = configuration.getConfigurationSection("permissions-for-actions");
        if (permForActionsSection != null) {
            for (String action : permForActionsSection.getKeys(false)) {
                List<String> permissions = permForActionsSection.getStringList(action);
                permissionsForActions.put(action, permissions);
            }
        }
        // update permissions for commands
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
        // save permissions for actions if needed
        if (needSave) {
            configuration.set("permissions-for-actions", permissionsForActions);
            configuration.save(configPath.toFile());
        }
    }

    public Map<String, String> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public Map<String, List<String>> getPermissionsForActions() {
        return Collections.unmodifiableMap(permissionsForActions);
    }

    public int getParseLoopLimit() {
        return parseLoopLimit;
    }
}
