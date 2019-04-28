package com.github.kuro46.embedscript.script

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Script manager<br></br>
 * This class is thread-safe
 *
 * @author shirokuro
 */
class ScriptManager
private constructor(val scripts: ListMultimap<ScriptPosition, Script>, val path: Path) {
    operator fun contains(position: ScriptPosition): Boolean {
        return scripts.containsKey(position)
    }

    operator fun get(position: ScriptPosition): MutableList<Script> {
        return scripts.get(position)
    }

    fun put(position: ScriptPosition, script: Script) {
        val scripts = scripts.get(position)
        scripts.add(script)
        ScriptSerializer.serializeLaterAsync(path, this.scripts)
    }

    fun putIfAbsent(position: ScriptPosition, script: Script) {
        if (scripts.containsKey(position)) {
            return
        }
        val scripts = scripts.get(position)
        scripts.add(script)
        ScriptSerializer.serializeLaterAsync(path, this.scripts)
    }

    fun remove(position: ScriptPosition): List<Script>? {
        val s = scripts.removeAll(position)
        ScriptSerializer.serializeLaterAsync(path, scripts)
        return s
    }

    fun keySet(): Set<ScriptPosition> {
        return scripts.keySet()
    }

    fun entries(): MutableCollection<MutableMap.MutableEntry<ScriptPosition, Script>> {
        return scripts.entries()
    }

    fun reload() {
        val scripts = ScriptSerializer.deserialize(path)
        this.scripts.clear()
        this.scripts.putAll(scripts)
    }

    fun save() {
        ScriptSerializer.serialize(path, scripts)
    }

    fun saveAsync() {
        ScriptSerializer.serializeLaterAsync(path, scripts)
    }

    companion object {
        fun load(filePath: Path): ScriptManager {
            val multimap =
                    Multimaps.newListMultimap(ConcurrentHashMap<ScriptPosition, CopyOnWriteArrayList<Script>>()
                            as Map<ScriptPosition, MutableCollection<Script>>) { CopyOnWriteArrayList() }
            multimap.putAll(ScriptSerializer.deserialize(filePath))
            return ScriptManager(multimap, filePath)
        }
    }
}
