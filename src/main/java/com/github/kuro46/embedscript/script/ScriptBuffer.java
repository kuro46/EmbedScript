package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.util.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ScriptBuffer {
    // Element order is important
    private final LinkedHashMap<String,List<String>> script;

    public ScriptBuffer(String string) throws ParseException{
        String[] keyValueStrings = Util.splitAndUnescape(string, "@");
        LinkedHashMap<String,List<String>> script = new LinkedHashMap<>();

        for (String keyValueString : keyValueStrings) {
            keyValueString = keyValueString.trim();
            if (keyValueString.isEmpty()){
                continue;
            }
            KeyValue keyValue = splitToKeyValue(keyValueString);
            script.put(keyValue.key,keyValue.values);
        }

        this.script = script;
    }

    public void clear() {
        script.clear();
    }

    public List<String> put(String key, List<String> values){
        return script.put(key.toLowerCase(Locale.ENGLISH), values);
    }

    public List<String> get(String key){
        List<String> list = script.get(key.toLowerCase(Locale.ENGLISH));
        return list == null ? null : Collections.unmodifiableList(list);
    }

    public List<String> remove(String key){
        return script.remove(key);
    }

    public void merge(ScriptBuffer other){
        this.script.putAll(other.script);
    }

    public Map<String,List<String>> unmodifiableMap(){
        LinkedHashMap<String,List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : script.entrySet()) {
            map.put(entry.getKey(),Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Split string to KeyValue
     *
     * @param string expects "key [value1][value2]", "key [value]" or "key value"
     * @return KeyValue
     */
    private KeyValue splitToKeyValue(String string) throws ParseException{
        String[] splitBySpace = string.split(" ");
        if (splitBySpace.length < 1){
            throw new ParseException("Failed to parse '" + string + "' to KeyValue");
        }
        // expect "key"
        String key = splitBySpace[0];
        // expect "", "value", "[value]", or "[value1][value2]"
        String value = Arrays.stream(splitBySpace)
            .skip(1)
            .collect(Collectors.joining(" "));

        List<String> values = splitValue(value);

        return new KeyValue(key,values);
    }

    private List<String> splitValue(String string) {
        if (string.isEmpty()) {
            return Collections.emptyList();
        }

        // translate color codes
        string = Util.replaceAndUnescape(string, "&(?<code>[0123456789AaBbCcDdEeFfKkLlMmNnOoRr])",
            "&${code}",
            "§${code}",
            false);
        if (string.startsWith("[") && string.endsWith("]")){
            // trim "[" and "]"
            string = string.substring(1, string.length() - 1);
            return Arrays.asList(Util.splitAndUnescape(string, "]["));
        }else {
            return Collections.singletonList(string);
        }
    }

    private static class KeyValue{
        private final String key;
        private final List<String> values;

        KeyValue(String key, List<String> values) {
            this.key = key;
            this.values = values;
        }
    }
}
