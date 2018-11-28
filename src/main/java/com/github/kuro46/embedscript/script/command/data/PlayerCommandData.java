package com.github.kuro46.embedscript.script.command.data;

import com.github.kuro46.embedscript.script.ScriptType;
import com.github.kuro46.embedscript.script.adapters.CommandDataAdapter;
import com.google.gson.annotations.JsonAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandDataAdapter.class)
public class PlayerCommandData extends CommandData {
    public PlayerCommandData() {
        super(ScriptType.PLAYER);
    }
}
