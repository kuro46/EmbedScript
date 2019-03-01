package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.ScriptBuffer;

import java.util.List;
import java.util.Map;

public class PresetProcessor extends AbstractProcessor {
    private final Configuration configuration;

    public PresetProcessor(Configuration configuration) {
        super("preset", "p");
        this.configuration = configuration;
    }

    @Override
    public void setup(ScriptBuffer source, String key, List<String> values) throws ParseException {
        ScriptBuffer mergeTo = null;
        Map<String, String> presets = configuration.getPresets();
        for (String value : values) {
            String preset = presets.get(value);
            if (preset == null){
                throw new ParseException(value + " is unknown preset!");
            }else {
                ScriptBuffer scriptBuffer = new ScriptBuffer(preset);

                if (mergeTo == null){
                    mergeTo = scriptBuffer;
                }else {
                    mergeTo.merge(scriptBuffer);
                }
            }
        }

        assert mergeTo != null;

        mergeTo.merge(source);
        source.clear();
        mergeTo.unmodifiableMap().forEach(source::put);
    }
}
