package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptGenerator;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.util.MojangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * @author shirokuro
 */
class Migrator {
    static void migrate(CommandSender commandSender,
                        ScriptUI scriptUI,
                        Plugin scriptBlock) throws Exception {
        for (Pair<EventType, Map<String, List<String>>> pair : getBlockList(scriptBlock)) {
            EventType eventType = pair.getKey();
            for (Map.Entry<String, List<String>> entry : pair.getValue().entrySet()) {
                List<String> data = entry.getValue();
                UUID author = getAuthorFromData(data);
                String commands = getCommandsFromData(data);

                Script script = ScriptGenerator.generateFromString(commandSender, author, commands);
                if (script == null) {
                    commandSender.sendMessage(Prefix.ERROR_PREFIX + "Failed to generate script!");
                    throw new RuntimeException("Failed to generate script!");
                }

                String rawLocation = entry.getKey();
                ScriptPosition position = createPositionFromRawLocation(rawLocation);

                if (scriptUI.hasScript(eventType, position)) {
                    scriptUI.add(commandSender, eventType, position, script);
                } else {
                    scriptUI.embed(commandSender, eventType, position, script);
                }
            }
        }
    }

    private static ScriptPosition createPositionFromRawLocation(String rawLocation) {
        //index0: world, 1: x, 2: y, 3: z
        String[] coordinates = rawLocation.split(",");
        return new ScriptPosition(coordinates[0],
            Integer.parseInt(coordinates[1]),
            Integer.parseInt(coordinates[2]),
            Integer.parseInt(coordinates[3]));
    }

    private static UUID getAuthorFromData(List<String> data) {
        //Author:MCID/Group → MCID/Group → MCID
        String authorName = data.get(0).split(":")[1].split("/")[0];
        return MojangUtil.getUUID(authorName);
    }

    private static String getCommandsFromData(List<String> data) {
        StringJoiner commandsJoiner = new StringJoiner("][", "[", "]");
        for (int i = 1; i < data.size(); i++)
            commandsJoiner.add(data.get(i));
        return commandsJoiner.toString();
    }

    private static List<Pair<EventType, Map<String, List<String>>>> getBlockList(Object scriptBlockInstance) throws Exception {

        Class<?> scriptManagerSuperClass = null;
        List<Object> scriptManagers = getField(scriptBlockInstance, "scriptManagerList");
        List<Pair<EventType, Map<String, List<String>>>> list = new ArrayList<>();

        for (int index = 0; index < scriptManagers.size(); index++) {
            Object scriptManager = scriptManagers.get(index);
            Class<?> scriptManagerClass = scriptManager.getClass();
            if (index == 0) {
                scriptManagerSuperClass = scriptManagerClass.getSuperclass();
            }

            Object mapManager = getField(scriptManagerSuperClass, scriptManager, "mapManager");
            Map<String, List<String>> blocksMap = getField(mapManager, "blocksMap");
            EventType eventType = getEventTypeByClass(scriptManagerClass);

            list.add(new Pair<>(eventType, blocksMap));
        }

        return list;
    }

    private static EventType getEventTypeByClass(Class clazz) throws Exception {
        switch (clazz.getSimpleName()) {
            case "PlayerInteractBlock":
                return EventType.INTERACT;
            case "PlayerWalkBlock":
                return EventType.WALK;
            default:
                throw new Exception(
                    String.format("Cannot get EventType from \"%s\" (Unknown class)", clazz.getSimpleName()));
        }
    }

    private static <T> T getField(Object object, String name) throws ReflectiveOperationException {
        return getField(object.getClass(), object, name);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Class clazz, Object object, String name) throws ReflectiveOperationException {
        Field declaredField = clazz.getDeclaredField(name);
        declaredField.setAccessible(true);
        return (T) declaredField.get(object);
    }

    private static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        K getKey() {
            return key;
        }

        V getValue() {
            return value;
        }
    }
}
