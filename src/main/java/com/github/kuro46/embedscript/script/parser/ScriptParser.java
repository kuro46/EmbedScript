package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;
import com.github.kuro46.embedscript.script.ScriptBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScriptParser {
    private final Configuration configuration;
    private final Processor[] processors;
    private int parseLoopCount = -1;

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
        this.configuration = configuration;
    }

    public Script parse(UUID author, String string) throws ParseException {
        ScriptBuffer source = new ScriptBuffer(string);
        // canonicalize and setup
        canonicalizeAndSetup(source);

        // processing phase
        ScriptBuilder builder = new ScriptBuilder(author);
        process(builder, source);

        Script built = builder.build();

        ScriptBuilder builderForFinalize = new ScriptBuilder(built);
        finalize(builderForFinalize);

        return builderForFinalize.build();
    }

    public void canonicalizeAndSetup(ScriptBuffer source) throws ParseException {
        if (isInParseLoop() && parseLoopCount++ > configuration.getParseLoopLimit()) {
            throw new ParseException("Limit of loop count exceeded while parsing!");
        }

        forEachProcessors(processor -> processor.canonicalize(this, source));
        forEachProcessors(processor -> processor.setup(this, source));
    }

    public void process(ScriptBuilder builder, ScriptBuffer source) throws ParseException {
        if (isInParseLoop() && parseLoopCount++ > configuration.getParseLoopLimit()) {
            throw new ParseException("Limit of loop count exceeded while parsing!");
        }

        forEachProcessors(processor -> processor.process(this, builder, source));
    }

    public void finalize(ScriptBuilder modifiableScript) throws ParseException {
        if (isInParseLoop() && parseLoopCount++ > configuration.getParseLoopLimit()) {
            throw new ParseException("Limit of loop count exceeded while parsing!");
        }

        forEachProcessors(processor -> processor.finalize(modifiableScript));
    }

    private void forEachProcessors(ProcessFunction<Processor> function) throws ParseException {
        for (Processor processor : processors) {
            boolean notInParseLoop = parseLoopCount == -1;
            if (notInParseLoop) {
                parseLoopCount = 0;
            }
            function.process(processor);
            if (notInParseLoop) {
                parseLoopCount = -1;
            }
        }
    }

    private boolean isInParseLoop() {
        return parseLoopCount != -1;
    }

    @FunctionalInterface
    private interface ProcessFunction<T> {
        void process(T t) throws ParseException;
    }
}
