package com.github.kuro46.embedscript

import com.google.gson.Gson

import java.lang.ref.WeakReference

/**
 * @author shirokuro
 */
object GsonHolder {
    private var gsonRef = WeakReference<Gson>(null)

    fun get(): Gson {
        var gson = gsonRef.get()
        if (gson == null) {
            gson = Gson()
            gsonRef = WeakReference(gson)
        }
        return gson
    }
}
