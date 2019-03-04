package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;

/**
 * Processes parse operation
 */
public interface Processor {
    void canonicalize(ScriptParser parser, ScriptBuffer source) throws ParseException;

    void setup(ScriptParser parser, ScriptBuffer source) throws ParseException;

    void process(ScriptParser parser, Script.Builder builder, ScriptBuffer source) throws ParseException;
}
