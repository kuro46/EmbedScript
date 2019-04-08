package com.github.kuro46.embedscript.script

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ScriptExporter(dataFolder: Path, private val scriptManager: ScriptManager) {
    private val dataFolder: Path = dataFolder.resolve("export")

    init {
        if (Files.notExists(this.dataFolder)) {
            Files.createDirectory(this.dataFolder)
        }
    }

    fun export(world: String, filePath: Path) {
        val exportTo = ScriptManager.load(filePath)

        for ((position, script) in scriptManager.entries()) {
            if (position.world == world) {
                exportTo.put(position, script)
            }
        }
    }

    fun resolveByExportFolder(other: String): Path {
        return dataFolder.resolve(other)
    }

    fun import(filePath: Path) {
        if (Files.notExists(filePath)) {
            throw IOException("'$filePath' not exists!")
        }
        val importFrom = ScriptManager.load(filePath)
        for ((position, script) in importFrom.entries()) {
            scriptManager.put(position, script)
        }
    }

    companion object {
        fun appendJsonExtensionIfNeeded(target: String): String {
            return if (target.endsWith(".json", ignoreCase = true)) target else "$target.json"
        }
    }
}
