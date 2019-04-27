package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.GsonHolder
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author shirokuro
 */
object ScriptSerializer {
    private val EXECUTOR = Executors.newSingleThreadScheduledExecutor { r ->
        val thread = Thread(r)
        thread.name = "EmbedScript IO Thread"
        thread.isDaemon = true

        thread
    }
    private const val LATEST_VERSION = "0.2.0"
    private val CREATORS: Map<String, (Path) -> Formatter>
    private var executing: ScheduledFuture<*>? = null

    init {
        val creators = HashMap<String, (Path) -> Formatter>()
        creators["1.0"] = { path -> Formatter10(path) }
        creators["0.2.0"] = { path -> Formatter020(path) }
        CREATORS = Collections.unmodifiableMap(creators)
    }

    @Synchronized
    fun deserialize(path: Path): ListMultimap<ScriptPosition, Script> {
        if (Files.notExists(path)) {
            return ArrayListMultimap.create()
        } else {
            Files.newBufferedReader(path).use { reader ->
                val version = readVersion(path)
                val formatter = createFormatter(version, path)
                        ?: throw UnsupportedOperationException("Unsupported version: $version")

                val result = formatter.fromJson(reader)
                if (version != LATEST_VERSION) {
                    serialize(path, result)
                }
                return result
            }
        }
    }

    @Synchronized
    fun serialize(path: Path, scripts: ListMultimap<ScriptPosition, Script>) {
        if (Files.notExists(path)) {
            Files.createFile(path)
        }

        Files.newBufferedWriter(path).use { writer ->
            val formatter = createFormatter(LATEST_VERSION, path) ?: throw IllegalStateException()
            formatter.toJson(writer, scripts)
        }
    }

    @Synchronized
    fun serializeLaterAsync(path: Path, scripts: ListMultimap<ScriptPosition, Script>) {
        if (executing != null) {
            executing!!.cancel(false)
        }

        executing = EXECUTOR.schedule({
            // "schedule" method do not notifies exception to UncaughtExceptionHandler
            try {
                serialize(path, scripts)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            synchronized(ScriptSerializer::class.java) {
                executing = null
            }
        }, 1, TimeUnit.SECONDS)
    }

    private fun createFormatter(version: String, path: Path): Formatter? {
        val formatterCreator = CREATORS[version] ?: return null
        return formatterCreator(path)
    }

    private fun readVersion(path: Path): String {
        if (Files.notExists(path)) {
            return LATEST_VERSION
        }

        JsonReader(Files.newBufferedReader(path)).use { reader ->
            var version: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "formatVersion") {
                    version = reader.nextString()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            if (version == null) {
                throw JsonSyntaxException("Illegal syntax")
            }

            return version
        }
    }

    private abstract class Formatter(
            protected val filePath: Path
    ) : TypeAdapter<ListMultimap<ScriptPosition, Script>>() {

        abstract fun version(): String
    }

    private class Formatter020(filePath: Path) : Formatter(filePath) {

        override fun version(): String {
            return "0.2.0"
        }

        override fun write(out: JsonWriter, value: ListMultimap<ScriptPosition, Script>) {
            out.beginObject()
            out.name("formatVersion").value(version())
            out.name("coordinates")
            writeCoordinates(out, value)
            out.endObject()
        }

        private fun writeCoordinates(out: JsonWriter, value: ListMultimap<ScriptPosition, Script>) {
            out.beginArray()
            for (position in value.keySet()) {
                val scripts = value.get(position)
                out.beginObject()
                out.name("coordinate").jsonValue(GsonHolder.get().toJson(position))
                out.name("scripts")
                writeScripts(out, scripts)
                out.endObject()
            }
            out.endArray()
        }

        private fun writeScripts(out: JsonWriter, scripts: List<Script>) {
            out.beginArray()
            for (script in scripts) {
                out.jsonValue(GsonHolder.get().toJson(script))
            }
            out.endArray()
        }

        override fun read(reader: JsonReader): ListMultimap<ScriptPosition, Script> {
            var scripts: ListMultimap<ScriptPosition, Script>? = null

            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "coordinates") {
                    scripts = readCoordinates(reader)
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            if (scripts == null)
                throw JsonSyntaxException("Illegal syntax")
            return scripts
        }

        private fun readCoordinates(reader: JsonReader): ListMultimap<ScriptPosition, Script> {
            val coordinates: ListMultimap<ScriptPosition, Script> = ArrayListMultimap.create()

            reader.beginArray()
            while (reader.hasNext()) {
                var position: ScriptPosition? = null
                var scripts: MutableList<Script>? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "coordinate" -> position = GsonHolder.get().fromJson(reader,
                                object : TypeToken<ScriptPosition>() {
                                }.type)
                        "scripts" -> {
                            scripts = ArrayList()
                            reader.beginArray()
                            while (reader.hasNext()) {
                                scripts.add(GsonHolder.get().fromJson(reader, object : TypeToken<Script>() {
                                }.type))
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                coordinates.putAll(position, scripts!!)
            }
            reader.endArray()

            return coordinates
        }
    }

    private class Formatter10(filePath: Path) : Formatter(filePath) {

        override fun version(): String {
            return "1.0"
        }

        override fun write(out: JsonWriter, value: ListMultimap<ScriptPosition, Script>) {
            throw UnsupportedOperationException("Outdated formatter.")
        }

        override fun read(reader: JsonReader): ListMultimap<ScriptPosition, Script> {
            var scripts: ListMultimap<ScriptPosition, Script>? = null

            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "scripts") {
                    scripts = readScripts(reader)
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            if (scripts == null)
                throw JsonSyntaxException("Illegal syntax")
            return scripts
        }

        private fun readScripts(reader: JsonReader): ListMultimap<ScriptPosition, Script> {
            val multimap: ListMultimap<ScriptPosition, Script> = ArrayListMultimap.create()

            reader.beginArray()
            while (reader.hasNext()) {
                val (position, scripts) = readPair(reader)
                multimap.putAll(position, scripts)
            }
            reader.endArray()

            return multimap
        }

        private fun readPair(reader: JsonReader): Pair<ScriptPosition, MutableList<Script>> {
            var position: ScriptPosition? = null
            var scripts: MutableList<Script>? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "coordinate" -> {
                        position = GsonHolder.get().fromJson(reader, object : TypeToken<ScriptPosition>() {

                        }.type)
                    }
                    "script" -> {
                        scripts = readScript(reader)
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()

            if (position == null || scripts == null)
                throw JsonSyntaxException("Illegal syntax.")
            return Pair(position, scripts)
        }

        private fun readScript(reader: JsonReader): MutableList<Script> {
            var eventType: EventType? = null
            for (type in EventType.values()) {
                val fileName = filePath.fileName ?: continue
                if (type.fileName == fileName.toString()) {
                    eventType = type
                }
            }
            if (eventType == null) {
                throw NullPointerException("Unknown path")
            }

            val scripts = ArrayList<Script>()
            reader.beginArray()
            while (reader.hasNext()) {
                var author: UUID? = null
                var command: String? = null
                val multimap: ListMultimap<String, String> = ArrayListMultimap.create()
                val keys: MutableList<String> = ArrayList()

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "author" -> author = UUID.fromString(reader.nextString())
                        "command" -> command = reader.nextString()
                        "data" -> {

                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "type" -> {
                                        when (val nextString = reader.nextString()) {
                                            "BYPASS_PERMISSION", "COMMAND" -> keys.add("command")
                                            "CONSOLE" -> keys.add("console")
                                            "PLAYER" -> keys.add("say")
                                            "PLUGIN" -> throw JsonParseException("@plugin was removed since ver0.7.0!")
                                            else -> throw JsonParseException("'$nextString' is unknown type!")
                                        }
                                    }
                                    "permission" -> multimap.put("give-permission", reader.nextString())
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                for (key in keys) {
                    multimap.put(key, command)
                }

                scripts.add(Script(author!!, -1,
                        if (eventType == EventType.WALK) setOf(Script.MoveType.GROUND) else setOf(),
                        if (eventType == EventType.INTERACT) setOf(Script.ClickType.ALL) else setOf(),
                        if (eventType == EventType.INTERACT) setOf(Script.PushType.ALL) else setOf(),
                        ImmutableListMultimap.copyOf(multimap)))
            }
            reader.endArray()

            return scripts
        }
    }
}
