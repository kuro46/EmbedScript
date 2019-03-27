package com.github.kuro46.embedscript.script;

import java.util.StringJoiner;

public final class ScriptUtil {
    private ScriptUtil() {
    }

    public static String toString(Iterable<String> values) {
        StringJoiner joiner = new StringJoiner("][", "[", "]");
        for (String value : values) {
            joiner.add(value);
        }
        return joiner.toString();
    }

    public static String toString(String value) {
        return '[' + value + ']';
    }
}
