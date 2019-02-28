package com.github.kuro46.embedscript.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScriptBuffer {
    private static final Pattern ARRAY_DELIMITER = Pattern.compile("([^\\\\])]\\[");
    private static final Pattern ESCAPED_ARRAY_DELIMITER = Pattern.compile("\\\\]\\[");
    private static final Pattern AT = Pattern.compile("([^\\\\])@");
    private static final Pattern ESCAPED_AT = Pattern.compile("\\\\@");

    // Element order is important
    private final LinkedHashMap<String,List<String>> script;

    public ScriptBuffer(String string) throws ParseException{
        List<String> keyValueStrings = splitByAt(string);
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

    public List<String> put(String key, List<String> values){
        return script.put(key, values);
    }

    public List<String> get(String key){
        List<String> list = script.get(key);
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
     * Split string by @
     *
     * @param string expects "@key [value1][value2] @key [value] @key value"
     * @return Strings
     */
    public List<String> splitByAt(String string){
        return splitAndUnescape(" " + string,"@",AT,ESCAPED_AT);
    }

    /**
     * Split string to KeyValue
     *
     * @param string expects "@key [value1][value2]", "@key [value]" or "@key value"
     * @return KeyValue
     */
    private KeyValue splitToKeyValue(String string) throws ParseException{
        String[] splitBySpace = string.split(" ");
        if (splitBySpace.length < 1){
            throw new ParseException("Failed to parse '" + string + "' to KeyValue");
        }
        // expect "@key"
        String key = splitBySpace[0];
        // expect "", "value", "[value]", or "[value1][value2]"
        String value = Arrays.stream(splitBySpace)
            .skip(1)
            .collect(Collectors.joining(" "));

        List<String> values = splitValue(value);

        return new KeyValue(key,values);
    }

    private String deleteAtFromKey(String key) throws ParseException{
        if (!key.startsWith("@")){
            throw new ParseException("Expected '@' at index 0 : " + key);
        }

        return key.substring(1);
    }

    private List<String> splitValue(String string){
        if (string.startsWith("[") && string.endsWith("]")){
            // trim "[" and "]"
            string = string.substring(1, string.length() - 1);
            return splitAndUnescape(string,"][",ARRAY_DELIMITER,ESCAPED_ARRAY_DELIMITER);
        }else if (string.isEmpty()){
            return Collections.emptyList();
        }else {
            return Collections.singletonList(string);
        }
    }

    private List<String> splitAndUnescape(String target,String replacement,Pattern pattern,Pattern escapedPattern){
        target = pattern.matcher(target).replaceAll("$1 " + replacement);
        return Arrays.stream(pattern.split(target))
            // unescape "\][" and unescape "\@"
            .map(s -> escapedPattern.matcher(s).replaceAll(replacement))
            .collect(Collectors.toList());
    }

    private static class KeyValue{
        private final String key;
        private final List<String> values;

        public KeyValue(String key, List<String> values) {
            this.key = key;
            this.values = values;
        }
    }
}
