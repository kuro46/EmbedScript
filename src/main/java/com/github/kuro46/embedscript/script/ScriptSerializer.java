package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.GsonHolder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author shirokuro
 */
public class ScriptSerializer {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("EmbedScript IO Thread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception occurred in " + t.getName() + " .");
            e.printStackTrace();
        });

        return thread;
    });
    private static final String LATEST_VERSION = "1.0";
    private static final Map<String, FormatterCreator> CREATORS;
    private static ScheduledFuture executing;

    static {
        Map<String, FormatterCreator> creators = new HashMap<>();
        creators.put("1.0", Formatter10::new);
        CREATORS = Collections.unmodifiableMap(creators);
    }

    public static Map<ScriptPosition, Script> deserialize(Path path) throws IOException {
        String version = readVersion(path);
        FormatterCreator formatterCreator = CREATORS.get(version);
        if (formatterCreator == null) {
            throw new UnsupportedOperationException("Unsupported version: " + version);
        }

        if (createFileIfNotExists(path)) {
            return new HashMap<>();
        } else {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                return formatterCreator.create().fromJson(reader);
            }
        }
    }

    public static void serialize(Path path, Map<ScriptPosition, Script> scripts) throws IOException {
        FormatterCreator formatterCreator = CREATORS.get(LATEST_VERSION);
        if (formatterCreator == null) {
            throw new IllegalStateException();
        }

        createFileIfNotExists(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            formatterCreator.create().toJson(writer, scripts);
        }
    }

    public static void serializeLaterAsync(Path path, Map<ScriptPosition, Script> scripts) {
        if (executing != null) {
            executing.cancel(false);
        }

        executing = EXECUTOR.schedule(() -> {
            try {
                serialize(path, scripts);
            } catch (IOException e) {
                e.printStackTrace();
            }

            executing = null;
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Create file if not exists.
     *
     * @param path path
     * @return true if created
     * @throws IOException IO exception
     */
    private static boolean createFileIfNotExists(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createFile(path);
            return true;
        }
        return false;
    }

    private static String readVersion(Path path) throws IOException {
        if (Files.notExists(path)) {
            return LATEST_VERSION;
        }

        try (BufferedReader bufferedReader = Files.newBufferedReader(path);
             JsonReader reader = new JsonReader(bufferedReader)) {
            String version = null;

            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("formatVersion")) {
                    version = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();

            if (version == null) {
                throw new JsonSyntaxException("Illegal syntax");
            }

            return version;
        }
    }

    private interface FormatterCreator {
        VersionedTypeAdapter create();
    }

    private static abstract class VersionedTypeAdapter extends TypeAdapter<Map<ScriptPosition, Script>> {
        public abstract String version();
    }

    private static class Formatter10 extends VersionedTypeAdapter {
        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public void write(JsonWriter out, Map<ScriptPosition, Script> value) throws IOException {
            out.beginObject();
            out.name("formatVersion").value(version());
            out.name("scripts");
            writeScripts(out, value);
            out.endObject();
        }

        private void writeScripts(JsonWriter out, Map<ScriptPosition, Script> scripts) throws IOException {
            out.beginArray();
            for (Map.Entry<ScriptPosition, Script> entry : scripts.entrySet()) {
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
        public Map<ScriptPosition, Script> read(JsonReader in) throws IOException {
            Map<ScriptPosition, Script> scripts = null;

            in.beginObject();
            while (in.hasNext()) {
                if (in.nextName().equals("scripts")) {
                    scripts = readScripts(in);
                } else {
                    in.skipValue();
                }
            }
            in.endObject();

            if (scripts == null)
                throw new JsonSyntaxException("Illegal syntax");
            return scripts;
        }

        private Map<ScriptPosition, Script> readScripts(JsonReader in) throws IOException {
            Map<ScriptPosition, Script> scripts = new HashMap<>();

            in.beginArray();
            while (in.hasNext()) {
                ScriptBlockScriptPair pair = readPair(in);
                scripts.put(pair.position, pair.script);
            }
            in.endArray();

            return scripts;
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

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ScriptBlockScriptPair that = (ScriptBlockScriptPair) o;
                return Objects.equals(position, that.position) &&
                    Objects.equals(script, that.script);
            }

            @Override
            public int hashCode() {
                return Objects.hash(position, script);
            }
        }
    }
}
