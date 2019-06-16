package com.github.kuro46.embedscript

import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.HashMap
import org.bukkit.configuration.file.YamlConfiguration

/**
 * @author shirokuro
 */
class Configuration private constructor(dataFolder: Path) {
    private var configurationData: ConfigurationData
    private val configPath: Path = dataFolder.resolve("config.yml")
    val presets: Presets
        get() = configurationData.presets
    val logConfiguration: LogConfiguration
        get() = configurationData.logConfiguration
    val cancelInteractEvent: Boolean
        get() = configurationData.cancelInteractEvent

    init {
        configurationData = load()
    }

    fun reload() {
        configurationData = load()
    }

    private fun load(): ConfigurationData {
        val configuration =
            Files.newBufferedReader(configPath)
                .use { reader -> YamlConfiguration.loadConfiguration(reader) }

        return ConfigurationData(
            configuration.getBoolean(KEY_CANCEL_INTERACT_EVENT),
            loadPresets(configuration),
            loadLogging(configuration)
        )
    }

    private fun loadLogging(configuration: org.bukkit.configuration.Configuration): LogConfiguration {
        return LogConfiguration(
            configuration.getBoolean(KEY_LOGGING_ENABLED),
            configuration.getString(KEY_LOGGING_FORMAT)
        )
    }

    private fun loadPresets(configuration: org.bukkit.configuration.Configuration): Presets {
        val presets = HashMap<String, String>()
        val presetsSection = configuration.getConfigurationSection(KEY_PRESETS)
        for (presetName in presetsSection.getKeys(false)) {
            val presetValue = presetsSection.getString(presetName)
            presets[presetName] = presetValue
        }

        return Collections.unmodifiableMap(presets)
    }

    companion object {
        private const val KEY_LOGGING_ENABLED = "logging.enabled"
        private const val KEY_LOGGING_FORMAT = "logging.format"
        private const val KEY_PRESETS = "presets"
        private const val KEY_CANCEL_INTERACT_EVENT = "cancel-interact-event"

        fun load(dataFolder: Path): Configuration {
            return Configuration(dataFolder)
        }
    }
}

private data class ConfigurationData(
    val cancelInteractEvent: Boolean,
    val presets: Presets,
    val logConfiguration: LogConfiguration
)

data class LogConfiguration(val enabled: Boolean, val format: String)

typealias Presets = Map<String, String>
