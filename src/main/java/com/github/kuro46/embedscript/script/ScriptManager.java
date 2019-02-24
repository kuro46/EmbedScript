package com.github.kuro46.embedscript.script;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Script manager<br>
 * This class is not thread-safe
 */
public class ScriptManager {
    private final Map<ScriptPosition, List<Script>> scripts;
    private final Path path;

    public ScriptManager(Map<ScriptPosition, List<Script>> scripts, Path path) {
        this.scripts = scripts;
        this.path = path;
    }

    public static ScriptManager load(Path filePath) throws IOException {
        return new ScriptManager(ScriptSerializer.deserialize(filePath), filePath);
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
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(this.scripts));
    }

    public void putIfAbsent(ScriptPosition position, Script script) {
        if (scripts.containsKey(position)) {
            return;
        }
        List<Script> scripts = getAndPutIfNeeded(position);
        scripts.add(script);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(this.scripts));
    }

    public List<Script> remove(ScriptPosition position) {
        List<Script> s = scripts.remove(position);
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(scripts));
        return s;
    }

    public Set<ScriptPosition> keySet() {
        return scripts.keySet();
    }

    public Set<Map.Entry<ScriptPosition, List<Script>>> entrySet() {
        return scripts.entrySet();
    }

    public void save() throws IOException {
        ScriptSerializer.serialize(path, new HashMap<>(scripts));
    }

    public void saveAsync() {
        ScriptSerializer.serializeLaterAsync(path, new HashMap<>(scripts));
    }

    /**
     * Returns snapshot of this instance. Return value is unmodifiable and thread-safe.
     *
     * @return Snapshot of this instance
     */
    public Map<ScriptPosition, List<Script>> snapshot() {
        Map<ScriptPosition, List<Script>> scripts = new HashMap<>();
        for (Map.Entry<ScriptPosition, List<Script>> entry : this.scripts.entrySet()) {
            ScriptPosition position = entry.getKey();
            List<Script> scriptList = entry.getValue();

            scripts.put(position, Collections.unmodifiableList(scriptList));
        }
        return Collections.unmodifiableMap(scripts);
    }
}
