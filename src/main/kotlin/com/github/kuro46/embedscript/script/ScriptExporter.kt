package com.github.kuro46.embedscript.script

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ScriptExporter(dataFolder: Path, private val scriptManager: ScriptManager) {
    private val exportFolder: Path = dataFolder.resolve("export")

    init {
        createDirectoryIfNotExists()
    }

    fun export(world: String, filePath: Path) {
        createDirectoryIfNotExists()

        val exportTo = ScriptManager.load(filePath)

        for ((position, script) in scriptManager.entries()) {
            if (position.world == world) {
                exportTo.put(position, script)
            }
        }
    }

    fun resolveByExportFolder(other: String): Path {
        return exportFolder.resolve(other)
    }

    fun import(filePath: Path) {
        createDirectoryIfNotExists()

        if (Files.notExists(filePath)) {
            throw IOException("'$filePath' not exists!")
        }
        val importFrom = ScriptManager.load(filePath)
        for ((position, script) in importFrom.entries()) {
            scriptManager.put(position, script)
        }
    }

    private fun createDirectoryIfNotExists() {
        if (Files.notExists(exportFolder)) {
            Files.createDirectory(exportFolder)
        }
    }

    companion object {
        fun appendJsonExtensionIfNeeded(target: String): String {
            return if (target.endsWith(".json", ignoreCase = true)) target else "$target.json"
        }
    }
}
