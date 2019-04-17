package com.github.kuro46.embedscript

import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class Configuration private constructor(dataFolder: Path) {

    private val configPath: Path = dataFolder.resolve("config.yml")
    var presets: Map<String, String>? = null
        private set
    var permissionsForActions: Map<String, List<String>>? = null
        private set
    private var parseLoopLimit: Int = 0
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

        // load parse-loop-limit
        parseLoopLimit = configuration.getInt(KEY_PARSE_LOOP_LIMIT, 3)

        loadLogging(configuration)

        loadPresets(configuration)

        loadPermissionsForActions(configuration)
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

    private fun loadPermissionsForActions(configuration: FileConfiguration) {
        // load
        val permissionsForActions = HashMap<String, List<String>>()
        val permForActionsSection = configuration.getConfigurationSection(KEY_PERMISSIONS_FOR_ACTIONS)
        if (permForActionsSection != null) {
            for (action in permForActionsSection.getKeys(false)) {
                val permissions = permForActionsSection.getStringList(action)
                permissionsForActions[action] = permissions
            }
        }
        // update
        var needSave = false
        for (plugin in Bukkit.getPluginManager().plugins) {
            val commands = plugin.description.commands ?: continue
            for ((command, value) in commands) {
                val permission = value["permission"] as String?

                if (permissionsForActions.containsKey(command)) {
                    continue
                }

                val permissions = permission?.let { listOf(it) }
                    ?: Collections.unmodifiableList(ArrayList<String>(0))
                // note:
                // please don't replace with Collections.emptyList()
                // because SnakeYaml cannot serialize Collections.emptyList()

                permissionsForActions[command] = permissions
                needSave = true
            }
        }
        // save if needed
        if (needSave) {
            configuration.set(KEY_PERMISSIONS_FOR_ACTIONS, permissionsForActions)
            configuration.save(configPath.toFile())
        }

        this.permissionsForActions = Collections.unmodifiableMap(permissionsForActions)
    }

    companion object {
        private const val KEY_PARSE_LOOP_LIMIT = "parse-loop-limit"
        private const val KEY_LOGGING_ENABLED = "logging.enabled"
        private const val KEY_LOGGING_FORMAT = "logging.format"
        private const val KEY_PRESETS = "presets"
        private const val KEY_PERMISSIONS_FOR_ACTIONS = "permissions-for-actions"

        fun load(dataFolder: Path): Configuration {
            return Configuration(dataFolder)
        }
    }
}
