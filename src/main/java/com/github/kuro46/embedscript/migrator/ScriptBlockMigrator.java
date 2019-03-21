package com.github.kuro46.embedscript.migrator;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.parser.ScriptParser;
import com.github.kuro46.embedscript.util.MojangUtil;
import com.github.kuro46.embedscript.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptBlockMigrator {
    private final ScriptParser parser;
    private final ScriptManager mergeTo;

    private ScriptBlockMigrator(Configuration configuration, ScriptManager mergeTo, Path dataFolder)
        throws IOException, InvalidConfigurationException, ParseException {

        this.mergeTo = mergeTo;
        this.parser = new ScriptParser(configuration);
        Path sbDataFolder = dataFolder.resolve(Paths.get("..", "ScriptBlock", "BlocksData"));
        for (EventType eventType : EventType.values()) {
            migrate(eventType, sbDataFolder.resolve(getSBFileName(eventType)));
        }
    }

    public static void migrate(Configuration configuration, ScriptManager mergeTo, Path dataFolder)
        throws InvalidConfigurationException, ParseException, IOException {

        new ScriptBlockMigrator(configuration, mergeTo, dataFolder);
    }

    private String getSBFileName(EventType eventType) {
        switch (eventType) {
            case WALK:
                return "walk_Scripts.yml";
            case INTERACT:
                return "interact_Scripts.yml";
            default:
                throw new IllegalArgumentException("Unsupported enum value!");
        }
    }

    private void migrate(EventType eventType, Path source) throws IOException, InvalidConfigurationException, ParseException {
        FileConfiguration configuration = loadScriptFile(source);
        for (String world : configuration.getKeys(false)) {
            ConfigurationSection worldSection = configuration.getConfigurationSection(world);
            for (String coordinate : worldSection.getKeys(false)) {
                List<String> dataList = worldSection.getStringList(coordinate);

                UUID author = getAuthorFromData(dataList.get(0));
                Script script = createScriptFromLegacyFormat(author, eventType, dataList.get(1));
                ScriptPosition position = createPositionFromRawLocation(world, coordinate);

                mergeTo.put(position, script);
            }
        }
    }

    private UUID getAuthorFromData(String data) throws ParseException{
        // Author:<MCID>/<Group>
        Matcher matcher = Pattern.compile("Author:(.+)/.+").matcher(data);
        if (!matcher.find()) {
            throw new ParseException("Illegal data");
        }
        String mcid = matcher.group(1);
        return MojangUtil.getUUID(mcid);
    }

    private FileConfiguration loadScriptFile(Path path) throws IOException, InvalidConfigurationException {
        FileConfiguration configuration = new YamlConfiguration();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            configuration.load(reader);
        }
        return configuration;
    }

    /**
     * Convert from legacy(ScriptBlock) format(e.g. '@command /cmd arg')<br>
     * Array(e.g. [@command /cmd1 arg][@bypass /cmd2 arg]) is unsupported
     *
     * @param author author of this script
     * @param legacy legacy format of script
     * @return script
     */
    private Script createScriptFromLegacyFormat(UUID author,
                                                       EventType eventType,
                                                       String legacy) throws ParseException {
        /*
         * Targets
         * @bypassperm:permission action
         * @command action
         * @player action
         */

        Pattern splitPattern = Pattern.compile("([^ ]+) (.+)");
        Matcher splitPatternMatcher = splitPattern.matcher(legacy);
        if (!splitPatternMatcher.find()) {
            throw new ParseException("Illegal script");
        }
        String actionType = splitPatternMatcher.group(1);
        String action = splitPatternMatcher.group(2);

        Map<String, String> formatBuilder = new HashMap<>();

        formatBuilder.put("@preset", eventType.getPresetName());
        formatBuilder.put("@action", action);

        switch (actionType.toLowerCase(Locale.ENGLISH)) {
            case "@command":
                formatBuilder.put("@action-type", "COMMAND");
                break;
            case "@player":
                formatBuilder.put("@action-type", "SAY");
                break;
            default:
                Pattern bypassPermPattern = Pattern.compile("^@bypassperm:(.+)", Pattern.CASE_INSENSITIVE);
                Matcher bypassPermPatternMatcher = bypassPermPattern.matcher(actionType);

                if (bypassPermPatternMatcher.find()) {
                    formatBuilder.put("@action-type", "COMMAND");
                    formatBuilder.put("@give-permission", bypassPermPatternMatcher.group(1));
                    break;
                }

                throw new ParseException(String.format("'%s' is unsupported action type!", actionType));
        }

        StringBuilder formattedByNewVersion = new StringBuilder();
        for (Map.Entry<String, String> entry : formatBuilder.entrySet()) {
            formattedByNewVersion.append(entry.getKey()).append(" ").append(entry.getValue()).append(" ");
        }
        // trim a space character at end of string
        String substring = formattedByNewVersion.substring(0, formattedByNewVersion.length() - 1);

        return parser.parse(author, substring);
    }

    private ScriptPosition createPositionFromRawLocation(String world, String rawLocation) {
        //index0: world, 1: x, 2: y, 3: z
        String[] coordinates = rawLocation.split(",");
        return new ScriptPosition(world,
            Integer.parseInt(coordinates[0]),
            Integer.parseInt(coordinates[1]),
            Integer.parseInt(coordinates[2]));
    }
}
