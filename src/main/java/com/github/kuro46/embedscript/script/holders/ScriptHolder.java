package com.github.kuro46.embedscript.script.holders;

import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.adapters.IScriptHolderAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author shirokuro
 */
@JsonAdapter(IScriptHolderAdapter.class)
public class ScriptHolder implements IScriptHolder {
    public static final String FORMAT_VERSION = "1.0";
    private final Map<ScriptPosition, Script> scripts = new HashMap<>();


    @Override
    public String formatVersion() {
        return FORMAT_VERSION;
    }


    public int size() {
        return scripts.size();
    }


    public boolean isEmpty() {
        return scripts.isEmpty();
    }


    public Script get(ScriptPosition key) {
        return scripts.get(key);
    }


    public boolean containsKey(ScriptPosition key) {
        return scripts.containsKey(key);
    }


    public Script put(ScriptPosition key, Script value) {
        return scripts.put(key, value);
    }


    public void putAll(Map<? extends ScriptPosition, ? extends Script> m) {
        scripts.putAll(m);
    }


    public Script remove(ScriptPosition key) {
        return scripts.remove(key);
    }


    public void clear() {
        scripts.clear();
    }


    public boolean containsValue(Script value) {
        return scripts.containsValue(value);
    }


    public Set<ScriptPosition> keySet() {
        return scripts.keySet();
    }


    public Collection<Script> values() {
        return scripts.values();
    }


    public Set<Map.Entry<ScriptPosition, Script>> entrySet() {
        return scripts.entrySet();
    }


    public Script getOrDefault(ScriptPosition key, Script defaultValue) {
        return scripts.getOrDefault(key, defaultValue);
    }


    public Script putIfAbsent(ScriptPosition key, Script value) {
        return scripts.putIfAbsent(key, value);
    }


    public boolean remove(ScriptPosition key, Script value) {
        return scripts.remove(key, value);
    }


    public boolean replace(ScriptPosition key, Script oldValue, Script newValue) {
        return scripts.replace(key, oldValue, newValue);
    }


    public Script replace(ScriptPosition key, Script value) {
        return scripts.replace(key, value);
    }


    public Script computeIfAbsent(ScriptPosition key, Function<? super ScriptPosition, ? extends Script> mappingFunction) {
        return scripts.computeIfAbsent(key, mappingFunction);
    }


    public Script computeIfPresent(ScriptPosition key, BiFunction<? super ScriptPosition, ? super Script, ? extends Script> remappingFunction) {
        return scripts.computeIfPresent(key, remappingFunction);
    }


    public Script compute(ScriptPosition key, BiFunction<? super ScriptPosition, ? super Script, ? extends Script> remappingFunction) {
        return scripts.compute(key, remappingFunction);
    }


    public Script merge(ScriptPosition key, Script value, BiFunction<? super Script, ? super Script, ? extends Script> remappingFunction) {
        return scripts.merge(key, value, remappingFunction);
    }


    public void forEach(BiConsumer<? super ScriptPosition, ? super Script> action) {
        scripts.forEach(action);
    }


    public void replaceAll(BiFunction<? super ScriptPosition, ? super Script, ? extends Script> function) {
        scripts.replaceAll(function);
    }
}
