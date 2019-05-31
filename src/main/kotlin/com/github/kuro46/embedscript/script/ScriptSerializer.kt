package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.json.JsonTableReader
import com.github.kuro46.embedscript.json.JsonTableWriter
import com.github.kuro46.embedscript.json.Metadata
import com.github.kuro46.embedscript.json.RecordWrite
import com.github.kuro46.embedscript.json.asType
import com.github.kuro46.embedscript.json.forEach
import com.github.kuro46.embedscript.json.getAsType
import com.github.kuro46.embedscript.util.ArrayListLinkedMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import com.google.gson.Gson
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
    fun deserialize(path: Path): ScriptManager {
        return if (Files.notExists(path)) {
            ScriptManager()
        } else {
            // exception if path is legacy format
            val tableReader = runCatching { JsonTableReader(path) }
            return tableReader.getOrNull()?.let { unwrappedTableWriter ->
                unwrappedTableWriter.use { usableWriter -> read(usableWriter) }
            } ?: LegacyParser.read(path)
        }
    }

    @Synchronized
    fun serialize(path: Path, scriptManager: ScriptManager) {
        if (Files.notExists(path)) {
            Files.createFile(path)
        }

        val metadata = Metadata()
        metadata.addProperty("formatVersion", "0.2.0")
        JsonTableWriter(path, metadata).use { write(it, scriptManager) }
    }

    @Synchronized
    fun serializeLaterAsync(path: Path, scriptManager: ScriptManager) {
        if (executing != null) {
            executing!!.cancel(false)
        }

        executing = EXECUTOR.schedule({
            // "schedule" method do not notifies exception to UncaughtExceptionHandler
            try {
                serialize(path, scriptManager)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            synchronized(ScriptSerializer::class.java) {
                executing = null
            }
        }, 1, TimeUnit.SECONDS)
    }

    private fun write(writer: JsonTableWriter, value: ScriptManager) {
        value.forEach { position, scriptList ->
            val record = RecordWrite()
            record.add("world", position.world)
            record.add("x", position.x)
            record.add("y", position.y)
            record.add("z", position.z)

            record.writer.name("scripts")
            record.writer.beginArray()
            scriptList.forEach { script ->
                record.writer.beginObject()

                record.add("author", script.author.toString())
                record.add("createdAt", script.createdAt)
                record.addObject("moveTypes", script.moveTypes)
                record.addObject("pushTypes", script.pushTypes)
                record.addObject("clickTypes", script.clickTypes)

                record.writer.name("script")
                record.writer.beginObject()
                script.keys.forEach { parentKeyData ->
                    parentKeyData.forEach { (key, values) ->
                        record.addObject(key, values)
                    }
                }
                record.writer.endObject()

                record.writer.endObject()
            }
            record.writer.endArray()

            writer.addRecord(record)
        }
    }

    private fun read(reader: JsonTableReader): ScriptManager {
        val result = ScriptManager()

        reader.forEach { record ->
            val position = ScriptPosition(
                record.getAsType("world"),
                record.getAsType("x"),
                record.getAsType("y"),
                record.getAsType("z")
            )

            val scripts = record.getAsJsonArray("scripts")
                .map { it.asJsonObject }
                .map { jsonScript ->
                    val scriptMultimap: ListMultimap<String, String> = ArrayListLinkedMultimap.create()
                    jsonScript.getAsJsonObject("script").forEach { key, value ->
                        scriptMultimap.putAll(key, value.asType<List<String>>())
                    }

                    @Suppress("UnstableApiUsage")
                    Script(
                        jsonScript.getAsType("createdAt"),
                        jsonScript.getAsType("author"),
                        ParentKeyData.fromMap(Multimaps.asMap(scriptMultimap)),
                        jsonScript.getAsType("clickTypes"),
                        jsonScript.getAsType("moveTypes"),
                        jsonScript.getAsType("pushTypes")
                    )
                }
                .toList()

            result.addAll(position, scripts)
        }
        return result
    }

    private class LegacyParser private constructor(val filePath: Path) {
        private val gson = Gson()

        fun read(reader: JsonReader): ScriptManager {
            val scripts = ScriptManager()

            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "scripts") {
                    readScripts(scripts, reader)
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            return scripts
        }

        private fun readScripts(
            scriptManager: ScriptManager,
            reader: JsonReader
        ) {
            reader.beginArray()
            while (reader.hasNext()) {
                val (position, scripts) = readPair(reader)
                for (script in scripts) {
                    scriptManager.add(position, script)
                }
            }
            reader.endArray()
        }

        private fun readPair(reader: JsonReader): Pair<ScriptPosition, MutableList<Script>> {
            var position: ScriptPosition? = null
            var scripts: MutableList<Script>? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "coordinate" -> {
                        position = gson.fromJson(
                            reader,
                            object : TypeToken<ScriptPosition>() {
                            }.type
                        )
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
                val multimap: ListMultimap<String, String> = ArrayListLinkedMultimap.create()
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
                                            "BYPASS_PERMISSION", "COMMAND" -> keys.add("cmd")
                                            "CONSOLE" -> keys.add("console")
                                            "PLAYER" -> keys.add("say")
                                            "PLUGIN" -> throw JsonParseException("@plugin was removed since ver0.7.0!")
                                            else -> throw JsonParseException("'$nextString' is unknown type!")
                                        }
                                    }
                                    "permission" -> multimap.put("cmd.bypass", reader.nextString())
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

                @Suppress("UnstableApiUsage")
                val script = Script(
                    -1,
                    author!!,
                    ParentKeyData.fromMap(Multimaps.asMap(multimap)),
                    if (eventType == EventType.INTERACT) setOf(ClickType.ALL) else setOf(),
                    if (eventType == EventType.WALK) setOf(MoveType.GROUND) else setOf(),
                    if (eventType == EventType.INTERACT) setOf(PushType.ALL) else setOf()
                )

                scripts.add(script)
            }
            reader.endArray()

            return scripts
        }

        companion object {
            fun read(filePath: Path): ScriptManager {
                val parser = LegacyParser(filePath)
                return JsonReader(Files.newBufferedReader(filePath)).use { parser.read(it) }
            }
        }
    }
}
