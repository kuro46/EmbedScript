package com.github.kuro46.embedscript.script;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Script manager<br>
 * This class is thread-safe
 */
public class ScriptManager {
    private final ConcurrentMap<ScriptPosition, List<Script>> scripts;
    private final Path path;

    public ScriptManager(ConcurrentMap<ScriptPosition, List<Script>> scripts, Path path) {
        this.scripts = scripts;
        this.path = path;
    }

    public static ScriptManager load(Path filePath) throws IOException {
        return new ScriptManager(new ConcurrentHashMap<>(ScriptSerializer.deserialize(filePath)), filePath);
    }

    public Path getPath() {
        return path;
    }

    private List<Script> getAndPutIfNeeded(ScriptPosition position) {
        return scripts.computeIfAbsent(position, ignore -> new ArrayList<>(1));
    }

    public boolean contains(ScriptPosition position) {
        return scripts.containsKey(position);
    }

    public List<Script> get(ScriptPosition position) {
        return scripts.getOrDefault(position, Collections.emptyList());
    }

    public void put(ScriptPosition position, Script script) {
        List<Script> scripts = getAndPutIfNeeded(position);
        scripts.add(script);
        ScriptSerializer.serializeLaterAsync(path, this.scripts);
    }

    public void putIfAbsent(ScriptPosition position, Script script) {
        if (scripts.containsKey(position)) {
            return;
        }
        List<Script> scripts = getAndPutIfNeeded(position);
        scripts.add(script);
        ScriptSerializer.serializeLaterAsync(path, this.scripts);
    }

    public List<Script> remove(ScriptPosition position) {
        List<Script> s = scripts.remove(position);
        ScriptSerializer.serializeLaterAsync(path, scripts);
        return s;
    }

    public Set<ScriptPosition> keySet() {
        return scripts.keySet();
    }

    public Set<Map.Entry<ScriptPosition, List<Script>>> entrySet() {
        return scripts.entrySet();
    }

    public void reload() throws IOException {
        Map<ScriptPosition, List<Script>> scripts = ScriptSerializer.deserialize(path);
        this.scripts.clear();
        this.scripts.putAll(scripts);
    }

    public void save() throws IOException {
        ScriptSerializer.serialize(path, scripts);
    }

    public void saveAsync() {
        ScriptSerializer.serializeLaterAsync(path, scripts);
    }

    public ConcurrentMap<ScriptPosition, List<Script>> getScripts() {
        return scripts;
    }
}
