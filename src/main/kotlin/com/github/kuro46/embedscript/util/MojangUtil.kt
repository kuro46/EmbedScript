package com.github.kuro46.embedscript.util

import com.google.common.cache.CacheBuilder
import com.google.gson.stream.JsonReader
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author shirokuro
 */
object MojangUtil {
    private val NAME_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<UUID, String>()
    private val UUID_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, UUID>()

    fun getName(uniqueId: UUID): String? {
        val cached = NAME_CACHE.getIfPresent(uniqueId)
        if (cached != null) {
            return cached
        }

        val stringUniqueId = uniqueId.toString().replace("-", "")
        val urlString = "https://api.mojang.com/user/profiles/$stringUniqueId/names"
        var name: String? = null

        try {
            newReader(urlString).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    name = readName(reader)
                }
                reader.endArray()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (name != null) {
            NAME_CACHE.put(uniqueId, name)
            return name
        }
        return null
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
        val cache = UUID_CACHE.getIfPresent(name)
        if (cache != null) {
            return cache
        }

        val stringUrl = "https://api.mojang.com/users/profiles/minecraft/$name"
        var stringUniqueId: String? = null

        try {
            newReader(stringUrl).use { reader ->
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
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (stringUniqueId != null) {
            val uniqueId = toUUID(stringUniqueId!!)
            UUID_CACHE.put(name, uniqueId)
            return uniqueId
        }
        return null
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
