package com.github.kuro46.embedscript.script

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author shirokuro
 */
class ScriptExporter(dataFolder: Path, private val scriptManager: ScriptManager) {
    private val exportFolder: Path = dataFolder.resolve("export")

    init {
        createDirectoryIfNotExists()
    }

    fun export(world: String, filePath: Path) {
        createDirectoryIfNotExists()

        val exportTo = ScriptManager(JsonLoader(filePath))

        scriptManager.forEach { position, scriptList ->
            if (position.world == world) {
                exportTo.putAll(position, scriptList)
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
        val importFrom = ScriptManager(JsonLoader(filePath))
        importFrom.forEach { position, scriptList ->
            scriptManager.putAll(position, scriptList)
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
