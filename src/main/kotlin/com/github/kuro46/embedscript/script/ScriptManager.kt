package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.util.COWList
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Script manager<br></br>
 * This class is thread-safe
 *
 * @author shirokuro
 */
class ScriptManager(private val loader: Loader = NoOpLoader) {

    private val lock = ReentrantLock()
    @Volatile
    private var scripts: Map<ScriptPosition, List<Script>> = loader.initialValue()

    private fun setScripts(scripts: Map<ScriptPosition, List<Script>>) {
        this.scripts = scripts
    }

    fun getScripts(): Map<ScriptPosition, List<Script>> {
        return scripts
    }

    // =============
    // IO OPERATIONS
    // =============

    fun reload() {
        val loaded = loader.load()
        setScripts(loaded.scripts)
    }

    fun save() {
        loader.save(this)
    }

    fun saveAsync() {
        loader.saveAsync(this)
    }

    // ===============
    // READ OPERATIONS
    // ===============

    operator fun contains(position: ScriptPosition): Boolean {
        return scripts.containsKey(position)
    }

    operator fun get(position: ScriptPosition): List<Script>? {
        return scripts[position]
    }

    fun keySet(): Set<ScriptPosition> {
        return scripts.keys
    }

    fun entries(): Collection<Map.Entry<ScriptPosition, List<Script>>> {
        return scripts.entries
    }

    // ================
    // WRITE OPERATIONS
    // ================

    private fun deepCopy(): MutableMap<ScriptPosition, COWList<Script>> {
        val copied = HashMap<ScriptPosition, COWList<Script>>()
        scripts.forEach { (position, scriptList) ->
            val cowList = if (scriptList is COWList<Script>)
                scriptList
            else COWList(scriptList)
            copied[position] = cowList
        }
        return copied
    }

    fun remove(position: ScriptPosition): List<Script>? {
        return lock.withLock {
            val scripts = deepCopy()
            val removed = scripts.remove(position)
            setScripts(scripts)
            loader.saveAsync(this, true)

            removed
        }
    }

    fun put(position: ScriptPosition, script: Script) {
        lock.withLock {
            val copied = deepCopy()
            val scriptList = copied.getOrPut(position) { COWList.empty() }
            scriptList.add(script)
            setScripts(copied)

            loader.saveAsync(this, true)
        }
    }

    fun putIfAbsent(position: ScriptPosition, script: Script) {
        lock.withLock {
            if (contains(position)) {
                return
            }
            val scripts = deepCopy()
            val scriptList = scripts.getOrPut(position) { COWList.empty() }
            scriptList.add(script)
            setScripts(scripts)
            loader.saveAsync(this, true)
        }
    }

    fun putAll(position: ScriptPosition, scripts: List<Script>) {
        lock.withLock {
            val copied = deepCopy()
            val scriptList = copied.getOrPut(position) { COWList.empty() }
            scriptList.addAll(scripts)
            setScripts(copied)

            loader.saveAsync(this, true)
        }
    }

    interface Loader {
        fun save(scriptManager: ScriptManager, autoSave: Boolean = false)
        fun saveAsync(scriptManager: ScriptManager, autoSave: Boolean = false)
        fun load(): ScriptManager
        fun initialValue(): Map<ScriptPosition, List<Script>>
    }
}

object NoOpLoader : ScriptManager.Loader {
    override fun save(scriptManager: ScriptManager, autoSave: Boolean) {
        if (autoSave) return
        throw UnsupportedOperationException()
    }

    override fun saveAsync(scriptManager: ScriptManager, autoSave: Boolean) {
        if (autoSave) return
        throw UnsupportedOperationException()
    }

    override fun load(): ScriptManager {
        throw UnsupportedOperationException()
    }

    override fun initialValue(): Map<ScriptPosition, List<Script>> {
        return emptyMap()
    }
}

class JsonLoader(val path: Path) : ScriptManager.Loader {
    override fun save(scriptManager: ScriptManager, autoSave: Boolean) {
        ScriptSerializer.serialize(path, scriptManager)
    }

    override fun saveAsync(scriptManager: ScriptManager, autoSave: Boolean) {
        ScriptSerializer.serializeLaterAsync(path, scriptManager)
    }

    override fun load(): ScriptManager {
        return ScriptSerializer.deserialize(path)
    }

    override fun initialValue(): Map<ScriptPosition, List<Script>> {
        return ScriptSerializer.deserialize(path).getScripts()
    }
}
