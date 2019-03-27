package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.util.Pair;
import com.github.kuro46.embedscript.util.Util;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MutableScript {
    private final ListMultimap<String, String> multimap = ArrayListMultimap.create();
    private ImmutableListMultimap<String, String> view;

    public MutableScript(String script) throws ParseException {
        script = escapeAtInSquareBrackets(script);
        String[] keyValueStrings = Util.splitAndUnescape(script, "@");

        for (String keyValueString : keyValueStrings) {
            keyValueString = keyValueString.trim();
            if (keyValueString.isEmpty()) {
                continue;
            }
            Pair<String, List<String>> pair = splitToKeyValue(keyValueString);
            for (String value : pair.getValue()) {
                add(pair.getKey(), value);
            }
        }
    }

    private String escapeAtInSquareBrackets(String string) {
        StringBuilder result = new StringBuilder(string.length());
        boolean inSquareBrackets = false;
        for (char c : string.toCharArray()) {
            if (c == '[') {
                inSquareBrackets = true;
            } else if (c == ']') {
                inSquareBrackets = false;
            }

            if (inSquareBrackets && c == '@') {
                result.append("\\@");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public void clear() {
        multimap.clear();
        invalidateView();
    }

    public void add(String key, String value) {
        multimap.put(key, value);
        invalidateView();
    }

    public void replaceKey(String value, String oldKey, String newKey) {
        while (multimap.remove(oldKey, value)) {
            multimap.put(newKey, value);
            invalidateView();
        }
    }

    public void putAllMapBehavior(MutableScript other) {
        for (String key : other.multimap.keySet()) {
            this.multimap.removeAll(key);
            this.multimap.putAll(key, other.multimap.get(key));
            invalidateView();
        }
    }

    private void invalidateView() {
        view = null;
    }

    public ImmutableListMultimap<String, String> getView() {
        if (view == null) {
            view = ImmutableListMultimap.copyOf(multimap);
        }
        return view;
    }

    /**
     * Split string to KeyValue
     *
     * @param string expects "key [value1][value2]", "key [value]" or "key value"
     * @return KeyValue
     */
    private Pair<String, List<String>> splitToKeyValue(String string) throws ParseException {
        Pair<String, String> pair = Util.splitByFirstSpace(string);
        if (pair == null) {
            throw new ParseException("Failed to parse '" + string + "' to KeyValue");
        }
        // expect "key"
        String key = pair.getKey().toLowerCase(Locale.ENGLISH);
        // expect "", "value", "[value]", or "[value1][value2]"
        String value = pair.getValue();

        List<String> values = splitValue(value);

        return new Pair<>(key, values);
    }

    private List<String> splitValue(String string) throws ParseException {
        if (string.isEmpty()) {
            return Collections.emptyList();
        } else if (!(string.startsWith("[") && string.endsWith("]"))) {
            throw new ParseException("Value of the script is needed to starts with '[' and ends with ']' : " + string);
        }

        // trim "[" and "]"
        string = string.substring(1, string.length() - 1);

        // translate color codes
        string = Util.replaceAndUnescape(string, "&(?<code>[0123456789AaBbCcDdEeFfKkLlMmNnOoRr])",
            "&${code}",
            "§${code}",
            false);

        return Arrays.asList(Util.splitAndUnescape(string, "]["));
    }
}
