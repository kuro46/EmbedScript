package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScriptParser {
    private final Processor[] processors;

    public ScriptParser(Configuration configuration) {
        List<Processor> processors = new ArrayList<>();
        processors.add(Processors.LISTEN_CLICK);
        processors.add(Processors.LISTEN_MOVE);
        processors.add(Processors.LISTEN_PUSH);
        processors.add(Processors.ENOUGH_PERMISSION);
        processors.add(Processors.NOT_ENOUGH_PERMISSION);
        processors.add(Processors.GIVE_PERMISSION);
        processors.add(Processors.ACTION_TYPE);
        processors.add(Processors.ACTION);
        processors.add(new PresetProcessor(configuration));
        this.processors = processors.toArray(new Processor[0]);
    }

    public Script parse(UUID author, String string) throws ParseException {
        ScriptBuffer source = new ScriptBuffer(string);
        // canonicalize phase
        for (Processor processor : processors) {
            processor.canonicalize(source);
        }

        // setup phase
        for (Processor processor : processors) {
            processor.setup(source);
        }

        // processing phase
        Script.Builder builder = new Script.Builder(author);
        for (Processor processor : processors) {
            processor.process(builder, source);
        }

        return builder.build();
    }
}
