package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.script.command.data.BypassPermCommandData;
import com.github.kuro46.embedscript.script.command.data.CommandCommandData;
import com.github.kuro46.embedscript.script.command.data.CommandData;
import com.github.kuro46.embedscript.script.command.data.ConsoleCommandData;
import com.github.kuro46.embedscript.script.command.data.PlayerCommandData;
import com.github.kuro46.embedscript.script.command.data.PluginCommandData;

import java.util.regex.Pattern;

/**
 * @author shirokuro
 */
public enum ScriptType {
    BYPASS_PERMISSION("@bypassperm") {
        private final transient Pattern pattern;
        {
            pattern = Pattern.compile(string + ":[^:]+", Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean match(String s) {
            return pattern.matcher(s).matches();
        }

        @Override
        public CommandData newDataFromString(String string) {
            if (!match(string)) {
                return null;
            }
            String permission = string.split(":")[1];
            return new BypassPermCommandData(permission);
        }
    },
    COMMAND("@command") {
        @Override
        public CommandData newDataFromString(String string) {
            return new CommandCommandData();
        }
    },
    CONSOLE("@console") {
        @Override
        public CommandData newDataFromString(String string) {
            return new ConsoleCommandData();
        }
    },
    PLAYER("@player") {
        @Override
        public CommandData newDataFromString(String string) {
            return new PlayerCommandData();
        }
    },
    PLUGIN("@plugin") {
        @Override
        public CommandData newDataFromString(String string) {
            return new PluginCommandData();
        }
    };

    protected final String string;

    ScriptType(String string) {
        this.string = string;
    }

    public static ScriptType getByString(String string) {
        for (ScriptType scriptType : values()) {
            if (scriptType.match(string)) {
                return scriptType;
            }
        }
        return null;
    }

    public boolean match(String s) {
        return s.equalsIgnoreCase(string);
    }

    public String getString() {
        return string;
    }

    public abstract CommandData newDataFromString(String string);

    @Override
    public String toString() {
        return "ScriptType{" +
            "string='" + string + '\'' +
            '}';
    }
}
