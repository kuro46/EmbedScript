package com.github.kuro46.embedscript.script

import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Script manager<br></br>
 * This class is thread-safe
 */
class ScriptManager(val scripts: ConcurrentMap<ScriptPosition, MutableList<Script>>, val path: Path) {

    private fun getAndPutIfNeeded(position: ScriptPosition): MutableList<Script> {
        return scripts.computeIfAbsent(position) { ArrayList(1) }
    }

    operator fun contains(position: ScriptPosition): Boolean {
        return scripts.containsKey(position)
    }

    operator fun get(position: ScriptPosition): List<Script> {
        return scripts.getOrDefault(position, emptyList<Script>().toMutableList())
    }

    fun put(position: ScriptPosition, script: Script) {
        val scripts = getAndPutIfNeeded(position)
        scripts.add(script)
        ScriptSerializer.serializeLaterAsync(path, this.scripts)
    }

    fun putIfAbsent(position: ScriptPosition, script: Script) {
        if (scripts.containsKey(position)) {
            return
        }
        val scripts = getAndPutIfNeeded(position)
        scripts.add(script)
        ScriptSerializer.serializeLaterAsync(path, this.scripts)
    }

    fun remove(position: ScriptPosition): List<Script>? {
        val s = scripts.remove(position)
        ScriptSerializer.serializeLaterAsync(path, scripts)
        return s
    }

    fun keySet(): Set<ScriptPosition> {
        return scripts.keys
    }

    fun entrySet(): MutableSet<MutableMap.MutableEntry<ScriptPosition, MutableList<Script>>> {
        return scripts.entries
    }

    @Throws(IOException::class)
    fun reload() {
        val scripts = ScriptSerializer.deserialize(path)
        this.scripts.clear()
        this.scripts.putAll(scripts)
    }

    @Throws(IOException::class)
    fun save() {
        ScriptSerializer.serialize(path, scripts)
    }

    fun saveAsync() {
        ScriptSerializer.serializeLaterAsync(path, scripts)
    }

    companion object {

        @Throws(IOException::class)
        fun load(filePath: Path): ScriptManager {
            return ScriptManager(ConcurrentHashMap(ScriptSerializer.deserialize(filePath)), filePath)
        }
    }
}
