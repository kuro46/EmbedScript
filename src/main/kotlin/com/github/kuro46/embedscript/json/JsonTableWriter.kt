package com.github.kuro46.embedscript.json

import com.google.gson.stream.JsonWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author shirokuro
 */
class RecordWrite {
    private val jsonBuffer = StringWriter()
    val writer = JsonWriter(jsonBuffer)
    private var isCloseCalled = false

    init {
        writer.beginObject()
    }

    fun add(key: String, value: String) {
        writer.name(key).value(value)
    }

    fun add(key: String, value: Number) {
        writer.name(key).value(value)
    }

    fun addJson(key: String, value: String) {
        writer.name(key).jsonValue(value)
    }

    fun addObject(key: String, value: Any) {
        writer.name(key).jsonValue(GsonExt.GSON.toJson(value))
    }

    /**
     * This method is called by JsonTableWriter
     */
    fun close() {
        if (isCloseCalled) {
            return
        }

        writer.endObject()
        writer.close()

        isCloseCalled = true
    }

    /**
     * This method is called by JsonTableWriter
     */
    fun toJson(): String {
        if (!isCloseCalled) {
            throw IllegalStateException("close method does not called yet")
        }
        return jsonBuffer.toString()
    }
}

/**
 * @author shirokuro
 */
class JsonTableWriter(
    private val writer: JsonWriter,
    metadata: Metadata
) : AutoCloseable {

    constructor(path: Path, metadata: Metadata) :
        this(JsonWriter(Files.newBufferedWriter(path)), metadata)

    init {
        writer.beginObject()
        writer.name("metadata").jsonValue(GsonExt.GSON.toJson(metadata))
        writer.name("body")
        writer.beginArray()
    }

    fun addRecord(record: RecordWrite) {
        record.close()
        writer.jsonValue(record.toJson())
    }

    override fun close() {
        writer.endArray()
        writer.endObject()
        writer.close()
    }
}
