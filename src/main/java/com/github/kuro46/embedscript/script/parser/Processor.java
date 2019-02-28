package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;

/**
 * Processes parse operation
 */
public interface Processor {
    void canonicalize(ScriptBuffer source) throws ParseException;
    void setup(ScriptBuffer source) throws ParseException;
    void process(Script.Builder builder,ScriptBuffer source) throws ParseException;
}
