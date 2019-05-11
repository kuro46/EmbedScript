package com.github.kuro46.embedscript.json

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.lang.ref.SoftReference

object GsonExt {
    private var gsonReference = SoftReference<Gson>(null)
    val GSON: Gson
        get() {
            return gsonReference.get() ?: run {
                val gson = Gson()
                gsonReference = SoftReference(gson)
                gson
            }
        }
}

inline fun <reified T> JsonObject.getAsType(key: String): T {
    return this.get(key).asType()
}

inline fun <reified T> JsonElement.asType(): T {
    return GsonExt.GSON.fromJson(
            this,
            object : TypeToken<T>() {
            }.type
    )
}

inline fun JsonObject.forEach(function: (String, JsonElement) -> Unit) {
    this.entrySet().forEach { (key, value) -> function(key, value) }
}
