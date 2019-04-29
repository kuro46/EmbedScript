package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.json.JsonTableReader
import com.github.kuro46.embedscript.json.JsonTableWriter
import com.github.kuro46.embedscript.json.Metadata
import com.github.kuro46.embedscript.json.RecordWrite
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
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
    private var executing: ScheduledFuture<*>? = null

    @Synchronized
    fun deserialize(path: Path): ListMultimap<ScriptPosition, Script> {
        return if (Files.notExists(path)) {
            ArrayListMultimap.create()
        } else {
            // exception if path is legacy format
            val tableReader = runCatching { JsonTableReader(path) }
            return tableReader.getOrNull()?.let { unwrappedTableWriter ->
                unwrappedTableWriter.use { usableWriter -> read(usableWriter) }
            } ?: LegacyParser.read(path)
        }
    }

    @Synchronized
    fun serialize(path: Path, scripts: ListMultimap<ScriptPosition, Script>) {
        if (Files.notExists(path)) {
            Files.createFile(path)
        }

        val metadata = Metadata()
        metadata.addProperty("formatVersion", "0.2.0")
        JsonTableWriter(path, metadata).use { write(it, scripts) }
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

    private fun write(writer: JsonTableWriter, value: ListMultimap<ScriptPosition, Script>) {
        value.entries().forEach { scriptData ->
            val position = scriptData.key
            val script = scriptData.value

            val record = RecordWrite()

            record.add("world", position.world)
            record.add("x", position.x)
            record.add("y", position.y)
            record.add("z", position.z)
            record.add("author", script.author.toString())
            record.add("createdAt", script.createdAt)
            val gson = writer.gson
            record.addJson("moveTypes", gson.toJson(script.moveTypes))
            record.addJson("pushTypes", gson.toJson(script.pushTypes))
            record.addJson("clickTypes", gson.toJson(script.clickTypes))

            record.writer.name("script")
            record.writer.beginObject()
            script.script.asMap().forEach { (key, values) ->
                record.addJson(key, gson.toJson(values))
            }
            record.writer.endObject()

            writer.addRecord(record)
        }
    }

    private fun read(reader: JsonTableReader): ListMultimap<ScriptPosition, Script> {
        val result: ListMultimap<ScriptPosition, Script> = ArrayListMultimap.create()

        reader.forEach { record ->
            val position = ScriptPosition(
                    record.get("world").asString,
                    record.get("x").asInt,
                    record.get("y").asInt,
                    record.get("z").asInt
            )

            val gson = reader.gson
            val scriptMultimap: ListMultimap<String, String> = ArrayListMultimap.create()
            record.getAsJsonObject("script").entrySet().forEach { (key, value) ->
                scriptMultimap.putAll(key, gson.fromJson<List<String>>(value))
            }

            val script = Script(
                    UUID.fromString(record.get("author").asString),
                    record.get("createdAt").asLong,
                    gson.fromJson(record.get("moveTypes")),
                    gson.fromJson(record.get("clickTypes")),
                    gson.fromJson(record.get("pushTypes")),
                    ImmutableListMultimap.copyOf(scriptMultimap)
            )

            result.put(position, script)
        }
        return result
    }

    private inline fun <reified T> Gson.fromJson(jsonElement: JsonElement): T {
        return this.fromJson(
                jsonElement,
                object : TypeToken<T>(){
                }.type
        )
    }

    private class LegacyParser private constructor(val filePath: Path) {
        private val gson = Gson()

        fun read(reader: JsonReader): ListMultimap<ScriptPosition, Script> {
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
                        position = gson.fromJson(reader,
                                object : TypeToken<ScriptPosition>() {
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

        companion object {
            fun read(filePath: Path): ListMultimap<ScriptPosition, Script> {
                val parser = LegacyParser(filePath)
                return JsonReader(Files.newBufferedReader(filePath)).use { parser.read(it) }
            }
        }
    }
}
