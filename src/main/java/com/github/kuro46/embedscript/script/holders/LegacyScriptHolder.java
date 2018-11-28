package com.github.kuro46.embedscript.script.holders;

import com.github.kuro46.embedscript.script.adapters.IScriptHolderAdapter;
import com.google.gson.annotations.JsonAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(IScriptHolderAdapter.class)
public class LegacyScriptHolder implements IScriptHolder {
    //DUMMY
    public static final String FORMAT_VERSION = "0.1";

    @Override
    public String formatVersion() {
        return FORMAT_VERSION;
    }
}
