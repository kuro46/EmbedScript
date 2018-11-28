package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.script.adapters.ScriptAdapter;
import com.github.kuro46.embedscript.script.command.Command;
import com.google.gson.annotations.JsonAdapter;

import java.util.List;

/**
 * @author shirokuro
 */
@JsonAdapter(ScriptAdapter.class)
public class Script {
    private final List<Command> commands;

    public Script(List<Command> commands) {
        this.commands = commands;
    }

    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public String toString() {
        return "Script{" +
            "commands=" + commands +
            '}';
    }
}
