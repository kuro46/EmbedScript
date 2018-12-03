package com.github.kuro46.embedscript.script.adapters;

import com.github.kuro46.embedscript.GsonHolder;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.holders.IScriptHolder;
import com.github.kuro46.embedscript.script.holders.LegacyScriptHolder;
import com.github.kuro46.embedscript.script.holders.ScriptHolder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author shirokuro
 */
public class IScriptHolderAdapter extends TypeAdapter<IScriptHolder> {
    @Override
    public void write(JsonWriter out, IScriptHolder value) throws IOException {
        out.beginObject();
        out.name("formatVersion").value(value.formatVersion());
        out.name("scripts");
        writeScripts(out, ((ScriptHolder) value).entrySet());
        out.endObject();
    }

    private void writeScripts(JsonWriter out, Set<Map.Entry<ScriptPosition, Script>> scripts) throws IOException {
        out.beginArray();
        for (Map.Entry<ScriptPosition, Script> entry : scripts) {
            writePair(out, entry.getKey(), entry.getValue());
        }
        out.endArray();
    }

    private void writePair(JsonWriter out, ScriptPosition position, Script script) throws IOException {
        out.beginObject();
        out.name("coordinate").jsonValue(GsonHolder.get().toJson(position));
        out.name("script").jsonValue(GsonHolder.get().toJson(script));
        out.endObject();
    }

    @Override
    public ScriptHolder read(JsonReader in) throws IOException {
        String serialVersion = null;
        List<ScriptBlockScriptPair> pairs = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "formatVersion": {
                    serialVersion = in.nextString();
                    break;
                }
                case "scripts": {
                    if (serialVersion == null) {
                        throw new JsonSyntaxException("serialVersion not found.");
                    }
                    switch (serialVersion) {
                        case ScriptHolder.FORMAT_VERSION: {
                            pairs = readPairs(in);
                            break;
                        }
                        case LegacyScriptHolder.FORMAT_VERSION: {
                            throw new UnsupportedOperationException("Unsupported format!");
                        }
                        default: {
                            throw new UnsupportedOperationException("Unsupported format!");
                        }
                    }
                    break;
                }
                default: {
                    in.skipValue();
                }
            }
        }
        in.endObject();

        if (pairs == null)
            throw new JsonSyntaxException("Illegal syntax");
        ScriptHolder scriptHolder = new ScriptHolder();
        pairs.forEach(pair -> scriptHolder.put(pair.position, pair.script));
        return scriptHolder;
    }

    private List<ScriptBlockScriptPair> readPairs(JsonReader in) throws IOException {
        List<ScriptBlockScriptPair> pairs = new ArrayList<>();

        in.beginArray();
        while (in.hasNext()) {
            pairs.add(readPair(in));
        }
        in.endArray();

        return pairs;
    }

    private ScriptBlockScriptPair readPair(JsonReader in) throws IOException {
        ScriptPosition position = null;
        Script script = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "coordinate": {
                    position = GsonHolder.get().fromJson(in, new TypeToken<ScriptPosition>() {
                    }.getType());
                    break;
                }
                case "script": {
                    script = GsonHolder.get().fromJson(in, new TypeToken<Script>() {
                    }.getType());
                    break;
                }
                default: {
                    in.skipValue();
                }
            }
        }
        in.endObject();

        if (position == null || script == null)
            throw new JsonSyntaxException("Illegal syntax.");
        return new ScriptBlockScriptPair(position, script);
    }

    private static class ScriptBlockScriptPair {
        private final ScriptPosition position;
        private final Script script;

        ScriptBlockScriptPair(ScriptPosition position, Script script) {
            this.position = position;
            this.script = script;
        }
    }
}
