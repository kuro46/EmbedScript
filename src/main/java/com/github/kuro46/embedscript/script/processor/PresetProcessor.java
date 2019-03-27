package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ParseException;
import com.google.common.collect.ImmutableList;

import java.util.Map;

public class PresetProcessor implements Processor {
    private final Parser parser;

    public PresetProcessor(Configuration configuration) {
        this.parser = new PresetParser(configuration);
    }

    @Override
    public String getKey() {
        return "preset";
    }

    @Override
    public String getOmittedKey() {
        return "p";
    }

    @Override
    public Parser getParser() {
        return parser;
    }

    @Override
    public Executor getExecutor() {
        return Processors.DEFAULT_EXECUTOR;
    }

    private static class PresetParser extends AbstractParser {
        private final Configuration configuration;

        public PresetParser(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void prepareBuild(ScriptProcessor processor, MutableScript script, String key, ImmutableList<String> matchedValues) throws ParseException {
            MutableScript mergeTo = null;
            Map<String, String> presets = configuration.getPresets();
            for (String value : matchedValues) {
                String preset = presets.get(value);
                if (preset == null) {
                    throw new ParseException("'" + value + "' is unknown preset!");
                } else {
                    MutableScript mutableScript = new MutableScript(preset);

                    processor.prepareBuild(mutableScript);

                    if (mergeTo == null) {
                        mergeTo = mutableScript;
                    } else {
                        mergeTo.putAllMapBehavior(mutableScript);
                    }
                }
            }

            assert mergeTo != null;

            mergeTo.putAllMapBehavior(script);
            script.clear();
            mergeTo.getView().forEach(script::add);
        }

        @Override
        public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException {
            // do nothing
            // please do not remove this method because AbstractParser#build does builder.getScript().putAll(key, matchedValues);
        }
    }
}
