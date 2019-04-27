package com.github.kuro46.embedscript

import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.HashMap

class Configuration private constructor(dataFolder: Path) {

    private val configPath: Path = dataFolder.resolve("config.yml")
    var presets: Map<String, String>? = null
        private set
    var isLogEnabled: Boolean = false
        private set
    var logFormat: String? = null
        private set

    init {
        load()
    }

    fun load() {
        val configuration = YamlConfiguration()
        Files.newBufferedReader(configPath).use { reader -> configuration.load(reader) }

        loadLogging(configuration)

        loadPresets(configuration)
    }

    private fun loadLogging(configuration: org.bukkit.configuration.Configuration) {
        isLogEnabled = configuration.getBoolean(KEY_LOGGING_ENABLED)
        logFormat = configuration.getString(KEY_LOGGING_FORMAT)
    }

    private fun loadPresets(configuration: org.bukkit.configuration.Configuration) {
        val presets = HashMap<String, String>()
        val presetsSection = configuration.getConfigurationSection(KEY_PRESETS)
        for (presetName in presetsSection.getKeys(false)) {
            val presetValue = presetsSection.getString(presetName)
            presets[presetName] = presetValue
        }
        this.presets = Collections.unmodifiableMap(presets)
    }

    companion object {
        private const val KEY_LOGGING_ENABLED = "logging.enabled"
        private const val KEY_LOGGING_FORMAT = "logging.format"
        private const val KEY_PRESETS = "presets"

        fun load(dataFolder: Path): Configuration {
            return Configuration(dataFolder)
        }
    }
}
