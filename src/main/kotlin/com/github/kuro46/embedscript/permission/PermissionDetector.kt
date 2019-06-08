package com.github.kuro46.embedscript.permission

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections

/**
 * @author shirokuro
 */
class PermissionDetector(parentPath: Path, fileName: String = "command_permissions.yml") {
    private val commandDataList: List<CommandData>
    val filePath = parentPath.resolve(fileName)

    init {
        if (Files.notExists(filePath)) {
            Files.createFile(filePath)

            val configuration = YamlConfiguration()
            fillPermissions(configuration)
            Files.newBufferedWriter(filePath).use { it.write(configuration.saveToString()) }
        }

        val configuration = Files.newBufferedReader(filePath)
            .use { YamlConfiguration.loadConfiguration(it) }
        @Suppress("UNCHECKED_CAST")
        val commandSections = configuration.get("commands") as List<Map<String, Any>>
        val result = ArrayList<CommandData>(commandSections.size)
        for (commandSection in commandSections) {
            @Suppress("UNCHECKED_CAST")
            result.add(CommandData(
                commandSection["name"] as String,
                commandSection["permissions"] as List<String>
            ))
        }

        commandDataList = result
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
        var filtered: List<CommandData> = commandDataList
        for (arg in args) {
            absolute.append(arg)

            val preFiltered =  filtered.filter { it.name.startsWith(absolute) }
            if (preFiltered.isEmpty()) {
                if (filtered.size > 1) {
                    throw IllegalStateException("Same named entries found.")
                }
                return filtered.flatMap { it.permissions }
            }
            filtered = preFiltered
        }

        return null
    }
}

private data class CommandData(val name: String, val permissions: List<String>)
