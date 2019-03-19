package com.github.kuro46.embedscript.migrator;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptBlockMigrator {
    private final ScriptManager mergeTo;

    private ScriptBlockMigrator(ScriptManager mergeTo, Path dataFolder) throws IOException, InvalidConfigurationException, ParseException {
        this.mergeTo = mergeTo;
        Path sbDataFolder = dataFolder.resolve(Paths.get("..", "ScriptBlock", "BlocksData"));
        for (EventType eventType : EventType.values()) {
            migrate(eventType, sbDataFolder.resolve(getSBFileName(eventType)));
        }
    }

    public static void migrate(ScriptManager mergeTo, Path dataFolder) throws InvalidConfigurationException, ParseException, IOException {
        new ScriptBlockMigrator(mergeTo, dataFolder);
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
    private static Script createScriptFromLegacyFormat(UUID author,
                                                       EventType eventType,
                                                       String legacy) throws ParseException {
        /*
         * Targets
         * @bypassperm:permission action
         * @command action
         * @player action
         */

        String[] split = legacy.split(" ");
        String actionTypeString = split[0].toLowerCase(Locale.ENGLISH);
        String action = Util.joinStringSpaceDelimiter(1, split);

        String permission = null;
        Script.ActionType actionType;

        switch (actionTypeString) {
            case "@command":
                actionType = Script.ActionType.COMMAND;
                break;
            case "@player":
                actionType = Script.ActionType.SAY;
                break;
            default:
                if (actionTypeString.startsWith("@bypassperm")) {
                    permission = actionTypeString.split(":")[1];
                    actionType = Script.ActionType.COMMAND;
                    break;
                }
                throw new ParseException(String.format("'%s' is unsupported action type!", actionTypeString));
        }

        Script.MoveType[] moveTypes = null;
        Script.ClickType[] clickTypes = null;
        Script.PushType[] pushTypes;

        switch (eventType) {
            case INTERACT:
                clickTypes = new Script.ClickType[]{Script.ClickType.ALL};
                pushTypes = new Script.PushType[]{Script.PushType.ALL};
                break;
            case WALK:
                moveTypes = new Script.MoveType[]{Script.MoveType.GROUND};
                pushTypes = new Script.PushType[0];
                break;
            default:
                throw new ParseException(String.format("'%s' is unsupported event type!", eventType));
        }

        return new Script(author,
            moveTypes == null ? new Script.MoveType[0] : moveTypes,
            clickTypes == null ? new Script.ClickType[0] : clickTypes,
            pushTypes,
            permission == null ? new String[0] : new String[]{permission},
            new String[0],
            new String[0],
            new Script.ActionType[]{actionType},
            new String[]{action});
    }

    private static ScriptPosition createPositionFromRawLocation(String world, String rawLocation) {
        //index0: world, 1: x, 2: y, 3: z
        String[] coordinates = rawLocation.split(",");
        return new ScriptPosition(world,
            Integer.parseInt(coordinates[0]),
            Integer.parseInt(coordinates[1]),
            Integer.parseInt(coordinates[2]));
    }
}
