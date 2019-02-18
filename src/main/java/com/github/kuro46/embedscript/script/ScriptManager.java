package com.github.kuro46.embedscript.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptManager {
    private static final Map<EventType, ScriptManager> MANAGERS = new EnumMap<>(EventType.class);

    private final Map<ScriptPosition,List<Script>> scripts;
    private final Path path;

    public ScriptManager(Map<ScriptPosition, List<Script>> scripts, Path path) {
        this.scripts = scripts;
        this.path = path;
    }

    public static void loadFiles(Path dataFolder) throws IOException{
        Files.createDirectories(dataFolder);

        for (EventType eventType : EventType.values()) {
            Path filePath = dataFolder.resolve(eventType.getFileName());
            MANAGERS.put(eventType,load(filePath));
        }
    }

    public static ScriptManager load(Path filePath) throws IOException{
        return new ScriptManager(ScriptSerializer.deserialize(filePath),filePath);
    }

    public static ScriptManager get(EventType eventType){
        return MANAGERS.get(eventType);
    }

    public Path getPath() {
        return path;
    }

    private List<Script> getAndPutIfNeeded(ScriptPosition position){
        return scripts.computeIfAbsent(position,ignore -> new ArrayList<>(1));
    }

    public boolean contains(ScriptPosition position){
        return scripts.containsKey(position);
    }

    public List<Script> get(ScriptPosition position){
        return scripts.getOrDefault(position,Collections.emptyList());
    }

    public void put(ScriptPosition position, Script script){
        List<Script> scripts = getAndPutIfNeeded(position);
        scripts.add(script);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(this.scripts));
    }

    public void putIfAbsent(ScriptPosition position, Script script){
        if (scripts.containsKey(position)){
            return;
        }
        List<Script> scripts = getAndPutIfNeeded(position);
        scripts.add(script);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(this.scripts));
    }

    public List<Script> remove(ScriptPosition position){
        List<Script> s = scripts.remove(position);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(scripts));
        return s;
    }

    public Set<ScriptPosition> keySet(){
        return scripts.keySet();
    }

    public Set<Map.Entry<ScriptPosition,List<Script>>> entrySet(){
        return scripts.entrySet();
    }

    public void save() throws IOException {
        ScriptSerializer.serialize(path,new HashMap<>(scripts));
    }

    public void saveAsync(){
        ScriptSerializer.serializeLaterAsync(path,new HashMap<>(scripts));
    }
}
