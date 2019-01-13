package com.github.kuro46.embedscript.api;

import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shirokuro
 */
public class EmbedScriptAPI {
    private static final Map<String, PerformListener> LISTENERS = new HashMap<>();
    private static final Map<String, PerformListener> LISTENERS_VIEW = Collections.unmodifiableMap(LISTENERS);

    public static boolean registerListener(Plugin plugin, String name, PerformListener listener) {
        String fullName = toFullName(plugin, name);
        if (LISTENERS.containsKey(fullName)) {
            return false;
        }

        LISTENERS.put(fullName, listener);
        return true;
    }

    public static PerformListener unregisterListener(Plugin plugin, String name) {
        return LISTENERS.remove(toFullName(plugin, name));
    }

    public static PerformListener getListener(Plugin plugin, String name) {
        return LISTENERS.get(toFullName(plugin, name));
    }

    public static PerformListener getListener(String fullName) {
        return LISTENERS.get(fullName);
    }

    public static Map<String, PerformListener> getListeners() {
        return LISTENERS_VIEW;
    }

    private static String toFullName(Plugin plugin, String name) {
        return plugin.getName() + '.' + name;
    }
}
