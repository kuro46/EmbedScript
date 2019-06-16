package com.github.kuro46.embedscript.permission

import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.file.YamlConfiguration

/**
 * @author shirokuro
 */
class PermissionDetector(parentPath: Path, fileName: String = "command_permissions.yml") {
    private var commandDataList: List<CommandData>
    val filePath = parentPath.resolve(fileName)

    init {
        if (Files.notExists(filePath)) {
            Files.createFile(filePath)

            val configuration = YamlConfiguration()
            fillPermissions(configuration)
            Files.newBufferedWriter(filePath).use { it.write(configuration.saveToString()) }
        }

        commandDataList = load()
    }

    private fun load(): List<CommandData> {
        val configuration =
            Files.newBufferedReader(filePath)
                .use { YamlConfiguration.loadConfiguration(it) }
        @Suppress("UNCHECKED_CAST")
        val commandSections = configuration.get("commands") as List<Map<String, Any>>
        val result = ArrayList<CommandData>(commandSections.size)
        for (commandSection in commandSections) {
            @Suppress("UNCHECKED_CAST")
            result.add(
                CommandData(
                    commandSection["name"] as String,
                    commandSection["permissions"] as List<String>
                )
            )
        }

        return result
    }

    fun reload() {
        commandDataList = load()
    }

    private fun fillPermissions(configuration: YamlConfiguration) {
        val sections = ArrayList<ConfigurationSection>()

        for (plugin in Bukkit.getPluginManager().plugins) {
            val commands = plugin.description.commands ?: continue
            for ((name, values) in commands) {
                if (!values.containsKey("permission")) {
                    continue
                }

                val section = MemoryConfiguration()
                section.set("name", name)
                section.set("permissions", ArrayList(Collections.singleton(values.getValue("permission"))))
                sections.add(section)
            }
        }

        configuration.set("commands", sections)
    }

    fun getPreferredPermission(args: List<String>): List<String>? {
        val absolute = StringBuilder()
        var permissions: List<String>? = null
        for (arg in args) {
            if (absolute.isNotEmpty()) {
                absolute.append(' ')
            }
            absolute.append(arg)

            val filtered = commandDataList.filter { it.name.contentEquals(absolute) }
            if (filtered.isNotEmpty()) {
                permissions = filtered.single().permissions
            }
        }

        return permissions
    }
}

private data class CommandData(val name: String, val permissions: List<String>)
