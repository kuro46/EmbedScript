package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractProcessor implements Processor{
    private final String key;
    private final String shortKey;

    public AbstractProcessor(String key, String shortKey) {
        this.key = key;
        this.shortKey = shortKey;
    }

    public AbstractProcessor() {
        this(null,null);
    }

    public boolean allowEmptyList(){
        return false;
    }

    public String getKey(){
        return Objects.requireNonNull(key,"key of AbstractProcessor is null!");
    }

    public String getShortKey(){
        return Objects.requireNonNull(shortKey,"key of AbstractProcessor is null!");
    }

    @Override
    public void canonicalize(ScriptBuffer source) {
        List<String> list = null;
        for (Map.Entry<String, List<String>> entry : source.unmodifiableMap().entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(getKey()) || key.equalsIgnoreCase(getShortKey())){
                list = entry.getValue();
            }
        }

        if (list == null){
            return;
        }

        source.remove(getShortKey());
        source.put(getKey(),list);
    }

    @Override
    public void setup(ScriptBuffer source) throws ParseException{
        for (Map.Entry<String, List<String>> entry : source.unmodifiableMap().entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (!allowEmptyList() && values.isEmpty()){
                throw new ParseException("Cannot set empty value to '" + key + "'.");
            }
            if (key.equals(getKey())) {
                setup(source, key, values);
            }
        }
    }

    public void setup(ScriptBuffer source, String key, List<String> values) throws ParseException{

    }

    @Override
    public void process(Script.Builder builder, ScriptBuffer source) throws ParseException {
        for (Map.Entry<String, List<String>> entry : source.unmodifiableMap().entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key.equals(getKey())) {
                process(builder, source, key, values);
            }
        }
    }

    public void process(Script.Builder builder, ScriptBuffer source,String key,List<String> values) throws ParseException{

    }
}
