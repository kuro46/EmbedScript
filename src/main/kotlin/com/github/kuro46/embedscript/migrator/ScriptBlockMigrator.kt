package com.github.kuro46.embedscript.migrator

import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.script.EventType
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtils
import com.github.kuro46.embedscript.util.MojangUtils
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.regex.Pattern

/**
 * @author shirokuro
 */
class ScriptBlockMigrator private constructor(embedScript: EmbedScript) {
    private val processor = embedScript.scriptProcessor
    private val mergeTo = embedScript.scriptManager

    init {
        val sbDataFolder = embedScript.dataFolder.resolve(Paths.get("..", "ScriptBlock", "BlocksData"))
        for (eventType in EventType.values()) {
            migrate(eventType, sbDataFolder.resolve(getSBFileName(eventType)))
        }
    }

    private fun getSBFileName(eventType: EventType): String {
        return when (eventType) {
            EventType.WALK -> "walk_Scripts.yml"
            EventType.INTERACT -> "interact_Scripts.yml"
        }
    }

    private fun migrate(eventType: EventType, source: Path) {
        val configuration = loadScriptFile(source)
        for (world in configuration.getKeys(false)) {
            val worldSection = configuration.getConfigurationSection(world)
            for (coordinate in worldSection.getKeys(false)) {
                val dataList = worldSection.getStringList(coordinate)

                val author = getAuthorFromData(dataList[0]) ?: throw ParseException("Failed to find author")
                val script = ScriptUtils.createScriptFromLegacyFormat(processor, author, eventType, dataList[1])
                val position = createPositionFromRawLocation(world, coordinate)

                mergeTo.add(position, script)
            }
        }
    }

    private fun getAuthorFromData(data: String): UUID? {
        // Author:<MCID>/<Group>
        val matcher = Pattern.compile("Author:(.+)/.+").matcher(data)
        if (!matcher.find()) {
            throw ParseException("Illegal data")
        }
        val mcid = matcher.group(1)
        return MojangUtils.getUUID(mcid)
    }

    private fun loadScriptFile(path: Path): FileConfiguration {
        val configuration = YamlConfiguration()
        Files.newBufferedReader(path).use { reader -> configuration.load(reader) }
        return configuration
    }

    private fun createPositionFromRawLocation(world: String, rawLocation: String): ScriptPosition {
        // index0: world, 1: x, 2: y, 3: z
        val coordinates = rawLocation.split(',').dropLastWhile { it.isEmpty() }
        return ScriptPosition(
            world,
            Integer.parseInt(coordinates[0]),
            Integer.parseInt(coordinates[1]),
            Integer.parseInt(coordinates[2])
        )
    }

    companion object {
        fun migrate(embedScript: EmbedScript) {

            ScriptBlockMigrator(embedScript)
        }
    }
}
