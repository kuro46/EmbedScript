package com.github.kuro46.embedscript.util

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.gson.stream.JsonReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * @author shirokuro
 */
object MojangUtils {
    private val NAME_CACHE: Cache<UUID, String> =
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build()
    private val UUID_CACHE: Cache<String, UUID> =
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build()

    fun getName(uniqueId: UUID): String? {
        NAME_CACHE.getIfPresent(uniqueId)?.let {
            return it
        }

        val url =
            uniqueId.toString().replace("-", "")
                .let { "https://api.mojang.com/user/profiles/$it/names" }
        var name: String? = null

        newReader(url).use { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                name = readName(reader)
            }
            reader.endArray()
        }

        return name?.let {
            NAME_CACHE.put(uniqueId, it)
            return it
        }
    }

    private fun readName(reader: JsonReader): String? {
        var name: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "name") {
                name = reader.nextString()
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()

        return name
    }

    fun getUUID(name: String): UUID? {
        UUID_CACHE.getIfPresent(name)?.let { return it }

        val url = "https://api.mojang.com/users/profiles/minecraft/$name"
        var stringUniqueId: String? = null

        newReader(url).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "id") {
                    stringUniqueId = reader.nextString()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
        }

        return stringUniqueId?.let {
            val uniqueId = toUUID(it)
            UUID_CACHE.put(name, uniqueId)
            uniqueId
        }
    }

    private fun newReader(urlString: String): JsonReader {
        val url = URL(urlString)
        return JsonReader(BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))
    }

    private fun toUUID(shortUUID: String): UUID {
        val sb = StringBuilder(36)
        sb.append(shortUUID)
        sb.insert(20, '-')
        sb.insert(16, '-')
        sb.insert(12, '-')
        sb.insert(8, '-')

        return UUID.fromString(sb.toString())
    }
}
