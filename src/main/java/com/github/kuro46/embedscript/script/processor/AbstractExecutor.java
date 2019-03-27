package com.github.kuro46.embedscript.script.processor;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;

public abstract class AbstractExecutor implements Processor.Executor {
    @Override
    public boolean check(Player trigger, ImmutableList<String> matchedValues) {
        return true;
    }

    @Override
    public void prepareExecute(Player trigger, ImmutableList<String> matchedValues) {

    }

    @Override
    public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {

    }

    @Override
    public void endExecute(Player trigger, ImmutableList<String> matchedValues) {

    }
}
