package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.script.Script;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ScriptBuilder {
    private final UUID author;
    private Set<Script.MoveType> moveTypes = new HashSet<>();
    private Set<Script.ClickType> clickTypes = new HashSet<>();
    private Set<Script.PushType> pushTypes = new HashSet<>();
    private ListMultimap<String, String> script = ArrayListMultimap.create();

    private ScriptBuilder(UUID author) {
        this.author = author;
    }

    public static ScriptBuilder withAuthor(UUID author) {
        return new ScriptBuilder(author);
    }

    public UUID getAuthor() {
        return author;
    }

    public Set<Script.MoveType> getMoveTypes() {
        return moveTypes;
    }

    public Set<Script.ClickType> getClickTypes() {
        return clickTypes;
    }

    public Set<Script.PushType> getPushTypes() {
        return pushTypes;
    }

    public ListMultimap<String, String> getScript() {
        return script;
    }

    public Script build() {
        return new Script(author,
            ImmutableSet.copyOf(moveTypes),
            ImmutableSet.copyOf(clickTypes),
            ImmutableSet.copyOf(pushTypes),
            ImmutableListMultimap.copyOf(script));
    }
}
