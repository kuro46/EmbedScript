package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.script.ParseException;
import com.google.common.collect.ImmutableList;

public abstract class AbstractParser implements Processor.Parser {
    @Override
    public void prepareBuild(ScriptProcessor processor, MutableScript buffer, String key, ImmutableList<String> matchedValues) throws ParseException {

    }

    @Override
    public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException {
        builder.getScript().putAll(key, matchedValues);
    }
}
