package com.github.kuro46.embedscript.json

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author shirokuro
 */
typealias RecordRead = JsonObject

/**
 * @author shirokuro
 */
class JsonTableReader(private val reader: JsonReader) : AutoCloseable, Iterator<RecordRead> {

    val gson = Gson()
    val metadata: Metadata

    constructor(path: Path): this(JsonReader(Files.newBufferedReader(path)))

    init {
        reader.beginObject()
        if (reader.nextName() != "metadata") {
            throw JsonSyntaxException("'metadata' section is must be placed at first object.")
        }
        metadata = gson.fromJson(reader, JsonObject::class.java)
        if (reader.nextName() != "body") {
            throw JsonSyntaxException("'body' section is must be placed at second object.")
        }
        reader.beginArray()
    }

    override fun hasNext(): Boolean = reader.hasNext()

    override fun next(): RecordRead = gson.fromJson(reader, JsonObject::class.java)

    override fun close() {
        reader.endArray()
        reader.endObject()
        reader.close()
    }
}
