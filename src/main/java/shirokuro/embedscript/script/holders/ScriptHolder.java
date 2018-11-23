package shirokuro.embedscript.script.holders;

import com.google.gson.annotations.JsonAdapter;
import shirokuro.embedscript.script.Script;
import shirokuro.embedscript.script.ScriptBlock;
import shirokuro.embedscript.script.adapters.IScriptHolderAdapter;

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
    private final Map<ScriptBlock, Script> scripts = new HashMap<>();


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


    public Script get(ScriptBlock key) {
        return scripts.get(key);
    }


    public boolean containsKey(ScriptBlock key) {
        return scripts.containsKey(key);
    }


    public Script put(ScriptBlock key, Script value) {
        return scripts.put(key, value);
    }


    public void putAll(Map<? extends ScriptBlock, ? extends Script> m) {
        scripts.putAll(m);
    }


    public Script remove(ScriptBlock key) {
        return scripts.remove(key);
    }


    public void clear() {
        scripts.clear();
    }


    public boolean containsValue(Script value) {
        return scripts.containsValue(value);
    }


    public Set<ScriptBlock> keySet() {
        return scripts.keySet();
    }


    public Collection<Script> values() {
        return scripts.values();
    }


    public Set<Map.Entry<ScriptBlock, Script>> entrySet() {
        return scripts.entrySet();
    }


    public Script getOrDefault(ScriptBlock key, Script defaultValue) {
        return scripts.getOrDefault(key, defaultValue);
    }


    public Script putIfAbsent(ScriptBlock key, Script value) {
        return scripts.putIfAbsent(key, value);
    }


    public boolean remove(ScriptBlock key, Script value) {
        return scripts.remove(key, value);
    }


    public boolean replace(ScriptBlock key, Script oldValue, Script newValue) {
        return scripts.replace(key, oldValue, newValue);
    }


    public Script replace(ScriptBlock key, Script value) {
        return scripts.replace(key, value);
    }


    public Script computeIfAbsent(ScriptBlock key, Function<? super ScriptBlock, ? extends Script> mappingFunction) {
        return scripts.computeIfAbsent(key, mappingFunction);
    }


    public Script computeIfPresent(ScriptBlock key, BiFunction<? super ScriptBlock, ? super Script, ? extends Script> remappingFunction) {
        return scripts.computeIfPresent(key, remappingFunction);
    }


    public Script compute(ScriptBlock key, BiFunction<? super ScriptBlock, ? super Script, ? extends Script> remappingFunction) {
        return scripts.compute(key, remappingFunction);
    }


    public Script merge(ScriptBlock key, Script value, BiFunction<? super Script, ? super Script, ? extends Script> remappingFunction) {
        return scripts.merge(key, value, remappingFunction);
    }


    public void forEach(BiConsumer<? super ScriptBlock, ? super Script> action) {
        scripts.forEach(action);
    }


    public void replaceAll(BiFunction<? super ScriptBlock, ? super Script, ? extends Script> function) {
        scripts.replaceAll(function);
    }
}
