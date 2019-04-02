package com.github.kuro46.embedscript.script

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ScriptExporter(private val dataFolder: Path, private val scriptManager: ScriptManager) {
    fun export(world: String, fileName: String): String {
        val exportTo = ScriptManager.load(dataFolder.resolve(fileName))

        for ((position, script) in scriptManager.entries()) {
            if (position.world == world) {
                exportTo.put(position, script)
            }
        }

        return fileName
    }

    fun import(fileName: String) {
        val filePath = dataFolder.resolve(fileName)
        if (Files.notExists(filePath)) {
            throw IOException("File: '$fileName' not exists!")
        }
        val importFrom = ScriptManager.load(filePath)
        for ((position, script) in importFrom.entries()) {
            scriptManager.put(position, script)
        }
    }
}
