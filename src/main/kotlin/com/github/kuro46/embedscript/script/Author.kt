package com.github.kuro46.embedscript.script

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.lang.System as JavaSystem
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.bukkit.Bukkit

sealed class Author {

    class Player(
        val uuid: UUID
    ) : Author() {

        private var expiredAt: Long = 0
        private var cachedName: String? = null

        override fun getAuthorName(): String {
            if (cachedName != null &&
                expiredAt > JavaSystem.currentTimeMillis()
            ) {
                return cachedName!!
            }

            val name = getLatestName()

            cachedName = name
            expiredAt = JavaSystem.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)

            return name
        }

        private fun getLatestName(): String {
            Bukkit.getPlayer(uuid)?.let { return it.name }

            val url =
                uuid.toString().replace("-", "")
                    .let { "https://api.mojang.com/user/profiles/$it/names" }
                    .let { URL(it) }

            val gson = Gson()
            val names =
                url
                    .openStream()
                    .bufferedReader()
                    .use { gson.fromJson(it, JsonArray::class.java) }

            val lastObject = names.last() as JsonObject
            return lastObject.get("name").asString
        }
    }

    class UnknownPlayer(val name: String) : Author() {
        override fun getAuthorName(): String {
            return name
        }
    }

    object System : Author() {
        override fun getAuthorName(): String {
            return "[System]"
        }
    }

    abstract fun getAuthorName(): String

    fun toJson(): JsonObject {
        val result = JsonObject()
        val kind = when (this) {
            is Player -> {
                result.addProperty("uuid", uuid.toString())
                "Player"
            }
            is UnknownPlayer -> {
                result.addProperty("name", name)
                "UnknownPlayer"
            }
            is System -> "System"
        }
        result.addProperty("kind", kind)

        return result
    }

    companion object {
        fun fromJson(json: JsonObject): Author {
            return when (val kind = json.get("kind").asString) {
                "Player" -> {
                    val uuidString = json.get("uuid").asString
                    val uuid = UUID.fromString(uuidString)
                    Player(uuid)
                }
                "UnknownPlayer" -> UnknownPlayer(json.get("name").asString)
                "System" -> System
                else -> throw IllegalArgumentException("Unknown kind: $kind")
            }
        }
    }
}