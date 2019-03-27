package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.script.ParseException;
import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;

public interface Processor {

    String getKey();

    String getOmittedKey();

    Parser getParser();

    Executor getExecutor();

    interface Parser {

        void prepareBuild(ScriptProcessor processor, MutableScript buffer, String key, ImmutableList<String> matchedValues) throws ParseException;

        void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException;
    }

    interface Executor {

        boolean check(Player trigger, ImmutableList<String> matchedValues);

        void prepareExecute(Player trigger, ImmutableList<String> matchedValues);

        void beginExecute(Player trigger, ImmutableList<String> matchedValues);

        void endExecute(Player trigger, ImmutableList<String> matchedValues);
    }
}
