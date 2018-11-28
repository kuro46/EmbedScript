package com.github.kuro46.embedscript.script.command.data;

import com.github.kuro46.embedscript.script.ScriptType;
import com.github.kuro46.embedscript.script.adapters.CommandDataAdapter;
import com.google.gson.annotations.JsonAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandDataAdapter.class)
public abstract class CommandData {
    protected final ScriptType type;

    public CommandData(ScriptType type) {
        this.type = type;
    }

    public ScriptType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "type=" + type +
            '}';
    }
}
