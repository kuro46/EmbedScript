package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptGenerator;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.util.MojangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * @author shirokuro
 */
public class Migrator {
    private final ScriptManager scriptManager;
    private final CommandSender commandSender;

    public Migrator(CommandSender commandSender, ScriptManager scriptManager, Plugin scriptBlock) throws ReflectiveOperationException {
        this.commandSender = commandSender;
        this.scriptManager = scriptManager;

        List<Object> scriptManagerList = getField(scriptBlock, "scriptManagerList");
        for (Object sbScriptManagerExtended : scriptManagerList) {
            Class<?> sbScriptManagerExtendedClass = sbScriptManagerExtended.getClass();
            Class<?> sbScriptManagerClass = sbScriptManagerExtendedClass.getSuperclass();
            Object mapManager = getField(sbScriptManagerClass, sbScriptManagerExtended, "mapManager");
            Map<String, List<String>> blocksMap = getField(mapManager, "blocksMap");

            EventType type;
            String simpleName = sbScriptManagerExtendedClass.getSimpleName();
            switch (simpleName) {
                case "PlayerInteractBlock": {
                    type = EventType.INTERACT;
                    break;
                }
                case "PlayerWalkBlock": {
                    type = EventType.WALK;
                    break;
                }
                default: {
                    throw new IllegalArgumentException(simpleName);
                }
            }
            migrate(type, blocksMap);
        }
    }

    private <T> T getField(Object object, String name) throws ReflectiveOperationException {
        return getField(object.getClass(), object, name);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Class clazz, Object object, String name) throws ReflectiveOperationException {
        Field declaredField = clazz.getDeclaredField(name);
        declaredField.setAccessible(true);
        return (T) declaredField.get(object);
    }

    private void migrate(EventType type, Map<String, List<String>> blocksMap) {
        blocksMap.forEach((locationSource, data) -> {
            String[] location = locationSource.split(",");
            //Author:MCID/Group → MCID/Group → MCID
            String authorName = data.get(0).split(":")[1].split("/")[0];
            UUID author = MojangUtil.getUUID(authorName);
            StringJoiner commandsJoiner = new StringJoiner("][", "[", "]");
            for (int i = 1; i < data.size(); i++)
                commandsJoiner.add(data.get(i));
            String commands = commandsJoiner.toString();

            Script script = ScriptGenerator.generateFromString(commandSender, author, commands);

            if (script == null) {
                commandSender.sendMessage(Prefix.ERROR_PREFIX + "Failed to generate script!");
                throw new RuntimeException("Failed to generate script!");
            }

            ScriptPosition position = new ScriptPosition(location[0],
                Integer.parseInt(location[1]),
                Integer.parseInt(location[2]),
                Integer.parseInt(location[3]));

            if (scriptManager.hasScript(type, position)) {
                scriptManager.add(commandSender, type, position, script);
            } else {
                scriptManager.embed(commandSender, type, position, script);
            }
        });
    }
}
