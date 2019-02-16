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
        thread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception occurred in " + t.getName() + " .");
            e.printStackTrace();
        });

        return thread;
    });
    private static final String LATEST_VERSION = "0.2.0";
    private static final Map<String, FormatterCreator> CREATORS;
    private static ScheduledFuture executing;

    static {
        Map<String, FormatterCreator> creators = new HashMap<>();
        creators.put("1.0", Formatter10::new);
        creators.put("0.2.0",Formatter020::new);
        CREATORS = Collections.unmodifiableMap(creators);
    }

    public static synchronized Map<ScriptPosition, List<Script>> deserialize(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new HashMap<>();
        } else {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String version = readVersion(path);
                Formatter formatter = createFormatter(version,path);
                if (formatter == null) {
                    throw new UnsupportedOperationException("Unsupported version: " + version);
                }
                return formatter.fromJson(reader);
            }
        }
    }

    public static synchronized void serialize(Path path, Map<ScriptPosition, List<Script>> scripts) throws IOException {
        if (Files.notExists(path)) {
            Files.createFile(path);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            Formatter formatter = createFormatter(LATEST_VERSION,path);
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

    private static Formatter createFormatter(String version,Path path) {
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
        Formatter create(Path path);
    }

    private static abstract class Formatter extends TypeAdapter<Map<ScriptPosition, List<Script>>> {
        protected final Path filePath;

        public Formatter(Path filePath) {
            this.filePath = filePath;
        }

        public abstract String version();
    }

    private static class Formatter020 extends Formatter{
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
            writeCoordinates(out,value);
            out.endObject();
        }

        private void writeCoordinates(JsonWriter out, Map<ScriptPosition, List<Script>> value) throws IOException{
            out.beginArray();
            for (Map.Entry<ScriptPosition, List<Script>> entry : value.entrySet()) {
                ScriptPosition position = entry.getKey();
                List<Script> scripts = entry.getValue();

                out.beginObject();
                out.name("coordinate").jsonValue(GsonHolder.get().toJson(position));
                out.name("scripts");
                writeScripts(out,scripts);
                out.endObject();
            }
            out.endArray();
        }

        private void writeScripts(JsonWriter out,List<Script> scripts) throws IOException{
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
                while (in.hasNext()){
                    switch (in.nextName()){
                        case "coordinate":
                            position = GsonHolder.get().fromJson(in,new TypeToken<ScriptPosition>() {
                            }.getType());
                            break;
                        case "scripts":
                            scripts = new ArrayList<>();
                            in.beginArray();
                            while (in.hasNext()){
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

                coordinates.put(position,scripts);
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

        private List<Script> readScript(JsonReader in) throws IOException{
            EventType eventType = null;
            for (EventType type : EventType.values()) {
                if (type.getFileName().equals(filePath.getFileName().toString())){
                    eventType = type;
                }
            }
            if (eventType == null){
                throw new NullPointerException("Unknown path");
            }

            List<Script> scripts = new ArrayList<>();
            in.beginArray();
            while (in.hasNext()){
                UUID author = null;
                String command = null;
                Script.ActionType actionType = null;
                String permission = null;

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
                            while (in.hasNext()){
                                switch (in.nextName()){
                                    case "type":
                                        switch (in.nextString()){
                                            case "BYPASS_PERMISSION":
                                            case "COMMAND":
                                                actionType = Script.ActionType.COMMAND;
                                                break;
                                            case "CONSOLE":
                                                actionType = Script.ActionType.CONSOLE;
                                                break;
                                            case "PLAYER":
                                                actionType = Script.ActionType.SAY;
                                                break;
                                            case "PLUGIN":
                                                actionType = Script.ActionType.PLUGIN;
                                                break;
                                        }
                                        break;
                                    case "permission":
                                        permission = in.nextString();
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

                scripts.add(new Script(author,
                    eventType == EventType.WALK ? new Script.MoveType[]{Script.MoveType.GROUND} : new Script.MoveType[0],
                    eventType == EventType.INTERACT ? new Script.ClickType[]{Script.ClickType.ALL} : new Script.ClickType[0],
                    eventType == EventType.INTERACT ? new Script.PushType[]{Script.PushType.ALL} : new Script.PushType[0],
                    permission == null ? new String[0] : new String[]{permission},
                    new String[0],
                    new String[0],
                    new Script.ActionType[]{actionType},
                    new String[]{command}));
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
