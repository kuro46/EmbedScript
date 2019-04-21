package com.github.kuro46.embedscript.migrator

import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.script.EventType
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.util.MojangUtil
import com.github.kuro46.embedscript.util.Util
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

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
                val script = createScriptFromLegacyFormat(author, eventType, dataList[1])
                val position = createPositionFromRawLocation(world, coordinate)

                mergeTo.put(position, script)
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
        return MojangUtil.getUUID(mcid)
    }

    private fun loadScriptFile(path: Path): FileConfiguration {
        val configuration = YamlConfiguration()
        Files.newBufferedReader(path).use { reader -> configuration.load(reader) }
        return configuration
    }

    /**
     * Convert from legacy(ScriptBlock) format(e.g. '@command /cmd arg')<br></br>
     * Array(e.g. [@command /cmd1 arg][@bypass /cmd2 arg]) is unsupported
     *
     * @param author author of this script
     * @param legacy legacy format of script
     * @return script
     */
    private fun createScriptFromLegacyFormat(author: UUID,
                                             eventType: EventType,
                                             legacy: String): Script {
        /*
         * Targets
         * @bypassperm:permission action
         * @command action
         * @player action
         * @bypass action
         */

        val pair = Util.splitByFirstSpace(legacy) ?: throw ParseException("Illegal script")
        val key = pair.first
        val value = "[${pair.second}]"

        val formatBuilder = HashMap<String, String>()

        formatBuilder["@preset"] = "[${eventType.presetName}]"

        when (key.toLowerCase(Locale.ENGLISH)) {
            "@command" -> formatBuilder["@command"] = value
            "@player" -> formatBuilder["@say"] = value
            "@bypass" -> formatBuilder["@preset"] = "[alternative-bypass]"
            else -> {
                val bypassPermPattern = Pattern.compile("^@bypassperm:(.+)", Pattern.CASE_INSENSITIVE)
                val bypassPermPatternMatcher = bypassPermPattern.matcher(key)

                if (bypassPermPatternMatcher.find()) {
                    formatBuilder["@console"] = value
                    formatBuilder["@give-permission"] = "[${bypassPermPatternMatcher.group(1)}]"
                } else {
                    throw ParseException("'$key' is unsupported action type!")
                }
            }
        }

        val formattedByNewVersion = StringBuilder()
        for ((key1, value1) in formatBuilder) {
            formattedByNewVersion.append(key1).append(" ").append(value1).append(" ")
        }
        // trim a space character at end of string
        val substring = formattedByNewVersion.substring(0, formattedByNewVersion.length - 1)

        return processor.parse(author, substring, -1)
    }

    private fun createPositionFromRawLocation(world: String, rawLocation: String): ScriptPosition {
        //index0: world, 1: x, 2: y, 3: z
        val coordinates = rawLocation.split(',').dropLastWhile { it.isEmpty() }
        return ScriptPosition(world,
                Integer.parseInt(coordinates[0]),
                Integer.parseInt(coordinates[1]),
                Integer.parseInt(coordinates[2]))
    }

    companion object {
        fun migrate(embedScript: EmbedScript) {

            ScriptBlockMigrator(embedScript)
        }
    }
}
