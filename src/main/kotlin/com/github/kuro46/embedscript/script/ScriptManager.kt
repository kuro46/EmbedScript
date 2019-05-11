package com.github.kuro46.embedscript.script

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

    fun forEach(function: (ScriptPosition, List<Script>) -> Unit) {
        getScripts().forEach(function)
    }

    // ================
    // WRITE OPERATIONS
    // ================

    private fun shallowCopy(): MutableMap<ScriptPosition, List<Script>> {
        return HashMap(scripts)
    }

    fun remove(position: ScriptPosition): List<Script>? {
        return lock.withLock {
            val scripts = shallowCopy()
            val removed = scripts.remove(position)
            setScripts(scripts)
            loader.saveAsync(this, true)

            removed
        }
    }

    fun add(position: ScriptPosition, script: Script) {
        lock.withLock {
            val copied = shallowCopy()
            copied.compute(position) { _, current ->
                val addTo = current?.let { ArrayList(it) } ?: ArrayList(1)
                addTo.add(script)
                addTo
            }

            setScripts(copied)

            loader.saveAsync(this, true)
        }
    }

    fun addAll(position: ScriptPosition, scripts: List<Script>) {
        lock.withLock {
            val copied = shallowCopy()
            copied.compute(position) { _, current ->
                val addTo: MutableList<Script> = current?.let {
                    val copiedList: MutableList<Script> = ArrayList(it.size + scripts.size)
                    copiedList.addAll(it)

                    copiedList
                } ?: ArrayList(scripts.size)

                addTo.addAll(scripts)

                addTo
            }

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
