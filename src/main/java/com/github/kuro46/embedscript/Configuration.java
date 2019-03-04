package com.github.kuro46.embedscript;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private final Path configPath;
    private Map<String,String> presets;
    private int parseLoopLimit;

    public Configuration(Path configPath) throws IOException, InvalidConfigurationException {
        this.configPath = configPath;
        load();
    }

    public void load() throws IOException, InvalidConfigurationException {
        presets = new HashMap<>();

        YamlConfiguration configuration = new YamlConfiguration();
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            configuration.load(reader);
        }

        parseLoopLimit = configuration.getInt("parse-loop-limit", 3);
        ConfigurationSection presetsSection = configuration.getConfigurationSection("presets");
        for (String presetName : presetsSection.getKeys(false)) {
            String presetValue = presetsSection.getString(presetName);
            presets.put(presetName,presetValue);
        }
    }

    public Map<String, String> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public int getParseLoopLimit() {
        return parseLoopLimit;
    }
}
