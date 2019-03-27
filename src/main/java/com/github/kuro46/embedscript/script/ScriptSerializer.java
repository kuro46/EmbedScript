package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.GsonHolder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.gson.JsonParseException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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

        return thread;
    });
    private static final String LATEST_VERSION = "0.2.0";
    private static final Map<String, FormatterCreator> CREATORS;
    private static ScheduledFuture executing;

    static {
        Map<String, FormatterCreator> creators = new HashMap<>();
        creators.put("1.0", Formatter10::new);
        creators.put("0.2.0", Formatter020::new);
        CREATORS = Collections.unmodifiableMap(creators);
    }

    public static synchronized Map<ScriptPosition, List<Script>> deserialize(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new HashMap<>();
        } else {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String version = readVersion(path);
                Formatter formatter = createFormatter(version, path);
                if (formatter == null) {
                    throw new UnsupportedOperationException("Unsupported version: " + version);
                }

                Map<ScriptPosition, List<Script>> result = formatter.fromJson(reader);
                if (!version.equals(LATEST_VERSION)) {
                    serialize(path, result);
                }
                return result;
            }
        }
    }

    public static synchronized void serialize(Path path, Map<ScriptPosition, List<Script>> scripts) throws IOException {
        if (Files.notExists(path)) {
            Files.createFile(path);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            Formatter formatter = createFormatter(LATEST_VERSION, path);
            if (formatter == null) {
                throw new IllegalStateException();
            }
            formatter.toJson(writer, scripts);
        }
    }

    public static synchronized void serializeLaterAsync(Path path, Map<ScriptPosition, List<Script>> scripts) {
        if (executing != null) {
            executing.cancel(false);
        }

        executing = EXECUTOR.schedule(() -> {
            // "schedule" method do not notifies exception to UncaughtExceptionHandler
            try {
                serialize(path, scripts);
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (ScriptSerializer.class) {
                executing = null;
            }
        }, 1, TimeUnit.SECONDS);
    }

    private static Formatter createFormatter(String version, Path path) {
        FormatterCreator formatterCreator = CREATORS.get(version);
        if (formatterCreator == null) {
            return null;
        }
        return formatterCreator.create(path);
    }

    private static String readVersion(Path path) throws IOException {
        if (Files.notExists(path)) {
            return LATEST_VERSION;
        }

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
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
        Formatter create(Path path);
    }

    private static abstract class Formatter extends TypeAdapter<Map<ScriptPosition, List<Script>>> {
        protected final Path filePath;

        public Formatter(Path filePath) {
            this.filePath = filePath;
        }

        public abstract String version();
    }

    private static class Formatter020 extends Formatter {
        public Formatter020(Path filePath) {
            super(filePath);
        }

        @Override
        public String version() {
            return "0.2.0";
        }

        @Override
        public void write(JsonWriter out, Map<ScriptPosition, List<Script>> value) throws IOException {
            out.beginObject();
            out.name("formatVersion").value(version());
            out.name("coordinates");
            writeCoordinates(out, value);
            out.endObject();
        }

        private void writeCoordinates(JsonWriter out, Map<ScriptPosition, List<Script>> value) throws IOException {
            out.beginArray();
            for (Map.Entry<ScriptPosition, List<Script>> entry : value.entrySet()) {
                ScriptPosition position = entry.getKey();
                List<Script> scripts = entry.getValue();

                out.beginObject();
                out.name("coordinate").jsonValue(GsonHolder.get().toJson(position));
                out.name("scripts");
                writeScripts(out, scripts);
                out.endObject();
            }
            out.endArray();
        }

        private void writeScripts(JsonWriter out, List<Script> scripts) throws IOException {
            out.beginArray();
            for (Script script : scripts) {
                out.jsonValue(GsonHolder.get().toJson(script));
            }
            out.endArray();
        }

        @Override
        public Map<ScriptPosition, List<Script>> read(JsonReader in) throws IOException {
            Map<ScriptPosition, List<Script>> scripts = null;

            in.beginObject();
            while (in.hasNext()) {
                if (in.nextName().equals("coordinates")) {
                    scripts = readCoordinates(in);
                } else {
                    in.skipValue();
                }
            }
            in.endObject();

            if (scripts == null)
                throw new JsonSyntaxException("Illegal syntax");
            return scripts;
        }

        private Map<ScriptPosition, List<Script>> readCoordinates(JsonReader in) throws IOException {
            Map<ScriptPosition, List<Script>> coordinates = new HashMap<>();

            in.beginArray();
            while (in.hasNext()) {
                ScriptPosition position = null;
                List<Script> scripts = null;

                in.beginObject();
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "coordinate":
                            position = GsonHolder.get().fromJson(in, new TypeToken<ScriptPosition>() {
                            }.getType());
                            break;
                        case "scripts":
                            scripts = new ArrayList<>();
                            in.beginArray();
                            while (in.hasNext()) {
                                scripts.add(GsonHolder.get().fromJson(in, new TypeToken<Script>() {
                                }.getType()));
                            }
                            in.endArray();
                            break;
                        default:
                            in.skipValue();
                    }
                }
                in.endObject();

                coordinates.put(position, scripts);
            }
            in.endArray();

            return coordinates;
        }
    }

    private static class Formatter10 extends Formatter {
        public Formatter10(Path filePath) {
            super(filePath);
        }

        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public void write(JsonWriter out, Map<ScriptPosition, List<Script>> value) throws IOException {
            throw new UnsupportedOperationException("Outdated formatter.");
        }

        @Override
        public Map<ScriptPosition, List<Script>> read(JsonReader in) throws IOException {
            Map<ScriptPosition, List<Script>> scripts = null;

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

        private Map<ScriptPosition, List<Script>> readScripts(JsonReader in) throws IOException {
            Map<ScriptPosition, List<Script>> scripts = new HashMap<>();

            in.beginArray();
            while (in.hasNext()) {
                ScriptBlockScriptPair pair = readPair(in);
                scripts.put(pair.position, pair.scripts);
            }
            in.endArray();

            return scripts;
        }

        private ScriptBlockScriptPair readPair(JsonReader in) throws IOException {
            ScriptPosition position = null;
            List<Script> scripts = null;

            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "coordinate": {
                        position = GsonHolder.get().fromJson(in, new TypeToken<ScriptPosition>() {
                        }.getType());
                        break;
                    }
                    case "script": {
                        scripts = readScript(in);
                        break;
                    }
                    default: {
                        in.skipValue();
                    }
                }
            }
            in.endObject();

            if (position == null || scripts == null)
                throw new JsonSyntaxException("Illegal syntax.");
            return new ScriptBlockScriptPair(position, scripts);
        }

        private List<Script> readScript(JsonReader in) throws IOException {
            EventType eventType = null;
            for (EventType type : EventType.values()) {
                Path fileName = filePath.getFileName();
                if (fileName == null) {
                    continue;
                }
                if (type.getFileName().equals(fileName.toString())) {
                    eventType = type;
                }
            }
            if (eventType == null) {
                throw new NullPointerException("Unknown path");
            }

            List<Script> scripts = new ArrayList<>();
            in.beginArray();
            while (in.hasNext()) {
                UUID author = null;
                String command = null;
                Multimap<String, String> multimap = ArrayListMultimap.create();
                List<String> keys = new ArrayList<>();

                in.beginObject();
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "author":
                            author = UUID.fromString(in.nextString());
                            break;
                        case "command":
                            command = in.nextString();
                            break;
                        case "data":

                            in.beginObject();
                            while (in.hasNext()) {
                                switch (in.nextName()) {
                                    case "type":
                                        String nextString = in.nextString();
                                        switch (nextString) {
                                            case "BYPASS_PERMISSION":
                                            case "COMMAND":
                                                keys.add("command");
                                                break;
                                            case "CONSOLE":
                                                keys.add("console");
                                                break;
                                            case "PLAYER":
                                                keys.add("say");
                                                break;
                                            case "PLUGIN":
                                                throw new JsonParseException("@plugin was removed since ver0.7.0!");
                                            default:
                                                throw new JsonParseException(
                                                    String.format("'%s' is unknown type!", nextString));
                                        }
                                        break;
                                    case "permission":
                                        multimap.put("give-permission", in.nextString());
                                        break;
                                    default:
                                        in.skipValue();
                                }
                            }
                            in.endObject();
                            break;
                        default:
                            in.skipValue();
                    }
                }
                in.endObject();

                for (String key : keys) {
                    multimap.put(key, command);
                }

                scripts.add(new Script(author,
                    eventType == EventType.WALK ? ImmutableSet.of(Script.MoveType.GROUND) : ImmutableSet.of(),
                    eventType == EventType.INTERACT ? ImmutableSet.of(Script.ClickType.ALL) : ImmutableSet.of(),
                    eventType == EventType.INTERACT ? ImmutableSet.of(Script.PushType.ALL) : ImmutableSet.of(),
                    ImmutableListMultimap.copyOf(multimap)));
            }
            in.endArray();

            return scripts;
        }

        private static class ScriptBlockScriptPair {
            private final ScriptPosition position;
            private final List<Script> scripts;

            ScriptBlockScriptPair(ScriptPosition position, List<Script> scripts) {
                this.position = position;
                this.scripts = scripts;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ScriptBlockScriptPair that = (ScriptBlockScriptPair) o;
                return Objects.equals(position, that.position) &&
                    Objects.equals(scripts, that.scripts);
            }

            @Override
            public int hashCode() {
                return Objects.hash(position, scripts);
            }
        }
    }
}
