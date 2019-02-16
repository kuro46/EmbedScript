package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.util.MojangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                List<Script> scripts = createScriptsFromData(author,eventType,data);

                String rawLocation = entry.getKey();
                ScriptPosition position = createPositionFromRawLocation(rawLocation);

                for (Script script : scripts) {
                    if (scriptUI.hasScript(position)) {
                        scriptUI.add(commandSender, position, script);
                    } else {
                        scriptUI.embed(commandSender, position, script);
                    }
                }
            }
        }
    }

    private static List<Script> createScriptsFromData(UUID author,EventType eventType,List<String> data) throws ParseException{
        List<Script> scripts = new ArrayList<>(data.size());
        for (int i = 1; i < data.size(); i++) {
            scripts.add(createScriptFromLegacyFormat(author,eventType,data.get(i)));
        }
        return scripts;
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
        String actionTypeString = split[0];
        String action = split[1];

        String permission = null;
        Script.ActionType actionType;

        switch (actionTypeString){
            case "@command":
                actionType = Script.ActionType.COMMAND;
                break;
            case "@player":
                actionType = Script.ActionType.SAY;
                break;
            default:
                if (actionTypeString.startsWith("@bypassperm")){
                    permission = actionTypeString.split(":")[1];
                    actionType = Script.ActionType.COMMAND;
                    break;
                }
                throw new ParseException(String.format("'%s' is unsupported action type!",actionTypeString));
        }

        Script.MoveType[] moveTypes = null;
        Script.ClickType[] clickTypes = null;
        Script.PushType[] pushTypes;

        switch (eventType){
            case INTERACT:
                clickTypes = new Script.ClickType[]{Script.ClickType.ALL};
                pushTypes = new Script.PushType[]{Script.PushType.ALL};
                break;
            case WALK:
                moveTypes = new Script.MoveType[]{Script.MoveType.GROUND};
                pushTypes = new Script.PushType[0];
                break;
            default:
                throw new ParseException(String.format("'%s' is unsupported event type!",eventType));
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
