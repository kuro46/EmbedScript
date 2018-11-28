package com.github.kuro46.embedscript.script.command.data;

import com.github.kuro46.embedscript.script.ScriptType;
import com.github.kuro46.embedscript.script.adapters.CommandDataAdapter;
import com.google.gson.annotations.JsonAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandDataAdapter.class)
public class BypassPermCommandData extends CommandData {
    private final String permission;

    public BypassPermCommandData(String permission) {
        super(ScriptType.BYPASS_PERMISSION);
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

    @Override
    public String toString() {
        return "BypassPermCommandData{" +
            "permission='" + permission + '\'' +
            ", type=" + type +
            '}';
    }
}
