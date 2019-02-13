package com.github.kuro46.embedscript.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScriptManager {
    private static final Map<EventType, ScriptManager> MANAGERS = new EnumMap<>(EventType.class);

    private final Map<ScriptPosition,Script> scripts;
    private final Path path;

    public ScriptManager(Map<ScriptPosition, Script> scripts, Path path) {
        this.scripts = scripts;
        this.path = path;
    }

    public static void load(Path dataFolder) throws IOException{
        Files.createDirectories(dataFolder);

        for (EventType eventType : EventType.values()) {
            Path filePath = dataFolder.resolve(eventType.getFileName());
            MANAGERS.put(eventType,new ScriptManager(ScriptSerializer.deserialize(filePath),filePath));
        }
    }

    public static ScriptManager get(EventType eventType){
        return MANAGERS.get(eventType);
    }

    public Path getPath() {
        return path;
    }

    public boolean contains(ScriptPosition position){
        return scripts.containsKey(position);
    }

    public Script get(ScriptPosition position){
        return scripts.get(position);
    }

    public Script put(ScriptPosition position, Script script){
        Script s = scripts.put(position, script);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(scripts));
        return s;
    }

    public Script putIfAbsent(ScriptPosition position, Script script){
        Script s = scripts.putIfAbsent(position, script);
        if (s == null){
            ScriptSerializer.serializeLaterAsync(path, new HashMap<>(scripts));
        }
        return s;
    }

    public Script remove(ScriptPosition position){
        Script s = scripts.remove(position);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(scripts));
        return s;
    }

    public Set<ScriptPosition> keySet(){
        return scripts.keySet();
    }

    public void save() throws IOException {
        ScriptSerializer.serialize(path,new HashMap<>(scripts));
    }

    public void saveAsync(){
        ScriptSerializer.serializeLaterAsync(path,new HashMap<>(scripts));
    }
}
