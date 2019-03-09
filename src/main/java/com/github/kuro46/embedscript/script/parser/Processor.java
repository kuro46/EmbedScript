package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.ScriptBuffer;
import com.github.kuro46.embedscript.script.ScriptBuilder;

/**
 * Processes parse operation
 */
public interface Processor {
    void canonicalize(ScriptParser parser, ScriptBuffer source) throws ParseException;

    void setup(ScriptParser parser, ScriptBuffer source) throws ParseException;

    void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source) throws ParseException;

    void finalize(ScriptBuilder modifiableScript) throws ParseException;
}
