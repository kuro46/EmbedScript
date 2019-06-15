package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.EmbedScript
import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.migrator.ScriptBlockMigrator
import com.github.kuro46.embedscript.permission.PermissionDetector
import com.github.kuro46.embedscript.request.Request
import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.script.ScriptExporter
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandHandlerManager
import com.github.kuro46.embedscript.util.command.ExecutionThreadType
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.LastArgument
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.LongArgumentInfo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.nio.file.Files
import kotlin.streams.toList

/**
 * @author shirokuro
 */
class ESCommandHandler constructor(
    embedScript: EmbedScript,
    commandHandlerManager: CommandHandlerManager
) {
    private val scriptProcessor = embedScript.scriptProcessor
    private val requests = embedScript.requests

    init {
        val scriptManager = embedScript.scriptManager
        val scriptExporter = embedScript.scriptExporter

        commandHandlerManager.registerHandler("embedscript help", HelpHandler())
        commandHandlerManager.registerHandler(
            "embedscript migrate",
            MigrateHandler(embedScript)
        )
        commandHandlerManager.registerHandler(
            "embedscript export",
            ExportHandler(scriptExporter)
        )
        commandHandlerManager.registerHandler(
            "embedscript import",
            ImportHandler(scriptExporter)
        )
        commandHandlerManager.registerHandler(
            "embedscript reload",
            ReloadHandler(
                embedScript.configuration,
                scriptManager,
                embedScript.permissionDetector
            )
        )
        commandHandlerManager.registerHandler(
            "embedscript teleport",
            TeleportHandler()
        )
        commandHandlerManager.registerHandler(
            "embedscript list",
            ListHandlers.ListHandler(scriptManager)
        )
        commandHandlerManager.registerHandler(
            "embedscript listAll",
            ListHandlers.ListAllHandler(scriptManager)
        )
        commandHandlerManager.registerHandler(
            "embedscript view",
            ViewHandler(requests, scriptManager)
        )
        commandHandlerManager.registerHandler(
            "embedscript remove",
            object : CommandHandler(
                ExecutionThreadType.ASYNCHRONOUS,
                ArgumentInfoList(
                    emptyList(),
                    LastArgument.NotAllow
                )
            ) {
                override fun handleCommand(
                    senderHolder: CommandSenderHolder,
                    args: Map<String, String>
                ) {
                    val player = senderHolder.tryCastToPlayerOrMessage() ?: return
                    player.sendMessage(Prefix.INFO + "Please click any block...")
                    requests.putRequest(player, Request.Remove)
                }
            }
        )
        commandHandlerManager.registerHandler(
            "embedscript embed",
            object : AbstractScriptCommandHandler(
                scriptProcessor,
                ExecutionThreadType.ASYNCHRONOUS,
                ArgumentInfoList(
                    emptyList(),
                    LongArgumentInfo("script", true)
                )
            ) {
                override fun handleCommand(
                    senderHolder: CommandSenderHolder,
                    args: Map<String, String>
                ) {
                    val player = senderHolder.tryCastToPlayerOrMessage() ?: return
                    modifyAction(player, args.getValue("script"), false)
                }
            }
        )
        commandHandlerManager.registerHandler(
            "embedscript add",
            object : AbstractScriptCommandHandler(
                scriptProcessor,
                ExecutionThreadType.ASYNCHRONOUS,
                ArgumentInfoList(
                    emptyList(),
                    LongArgumentInfo("script", true)
                )
            ) {
                override fun handleCommand(
                    senderHolder: CommandSenderHolder,
                    args: Map<String, String>
                ) {
                    val player = senderHolder.tryCastToPlayerOrMessage() ?: return
                    modifyAction(player, args.getValue("script"), true)
                }
            }
        )
    }

    private fun modifyAction(
        player: Player,
        stringScript: String,
        add: Boolean
    ): Boolean {
        val script = try {
            scriptProcessor.parse(System.currentTimeMillis(), player.uniqueId, stringScript)
        } catch (e: ParseException) {
            player.sendMessage(Prefix.ERROR + "Failed to parse script. ${e.message}")
            return true
        }

        player.sendMessage(Prefix.INFO + "Please click any block...")
        val request = if (add) Request.Add(script) else Request.Embed(script)
        requests.putRequest(player, request)

        return true
    }

    private class HelpHandler : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            LastArgument.NotAllow
        )
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            senderHolder.commandSender.sendMessage(
                """/es help - Displays this message.
                    |/es reload - Reloads configuration and scripts
                    |/es migrate - Migrates from ScriptBlock to this plugin.
                    |/es list [world] [page] - Displays list of scripts in the [world] or current world.
                    |/es listAll [page] - Displays list of scripts in this server.
                    |/es view - Displays information about script in the clicked block.
                    |/es view <world> <x> <y> <z> [page] - Displays information about scripts in the specified coordinate.
                    |/es remove - Removes all scripts in the clicked block.
                    |/es embed <script> - Embeds a script to the clicked block.
                    |/es add <script> - Adds a script to the clicked block
                    |/es export <world> [fileName] - Exports all scripts in the <world> to [fileName] or <world>.
                    |/es import <fileName> Imports all scripts in the <fileName>.""".trimMargin()
            )
        }
    }

    private class MigrateHandler(val embedScript: EmbedScript) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            LastArgument.NotAllow
        )
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val sender = senderHolder.commandSender
            sender.sendMessage(Prefix.INFO + "Migrating data of ScriptBlock...")
            val migrationResult = runCatching { ScriptBlockMigrator.migrate(embedScript) }
            migrationResult.exceptionOrNull()?.also {
                sender.sendMessage(Prefix.ERROR + "Migration failed!")
                System.err.println("Migration failed!")
                it.printStackTrace()
            } ?: run {
                sender.sendMessage(Prefix.SUCCESS + "Successfully migrated!")
            }
        }
    }

    private class ExportHandler(val scriptExporter: ScriptExporter) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            listOf(RequiedArgumentInfo("world")),
            OptionalArguments(listOf(OptionalArgumentInfo("fileName", null)))
        )
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val sender = senderHolder.commandSender

            sender.sendMessage(Prefix.INFO + "Exporting...")
            val world = args.getValue("world")
            val fileName = ScriptExporter.appendJsonExtensionIfNeeded(args.getOrElse("fileName") { world })
            val filePath = scriptExporter.resolveByExportFolder(fileName)

            if (Files.exists(filePath)) {
                sender.sendMessage(Prefix.ERROR + "File: '$fileName' already exists!")
            } else {
                scriptExporter.export(world, filePath)
                sender.sendMessage(
                    Prefix.SUCCESS +
                        "All scripts in the '$world' was successfully exported to '$fileName'!"
                )
            }
        }

        override fun handleTabComplete(
            senderHolder: CommandSenderHolder,
            commandName: String,
            completedArgs: List<String>,
            uncompletedArg: String
        ): List<String> {
            return if (completedArgs.isEmpty()) {
                // player wants world list
                Bukkit.getWorlds().stream()
                    .map { it.name }
                    .toList()
            } else {
                emptyList()
            }
        }
    }

    private class ImportHandler(val scriptExporter: ScriptExporter) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            listOf(RequiedArgumentInfo("fileName")),
            LastArgument.NotAllow
        )
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val sender = senderHolder.commandSender

            sender.sendMessage(Prefix.INFO + "Importing...")
            val fileName = args.getValue("fileName")
            val filePath = scriptExporter.resolveByExportFolder(fileName)
            if (Files.notExists(filePath)) {
                sender.sendMessage(Prefix.ERROR + "File: '$fileName' not exists!")
            } else {
                scriptExporter.import(filePath)
                sender.sendMessage(Prefix.SUCCESS + "Scripts were successfully imported from '$fileName'!")
            }
        }

        override fun handleTabComplete(
            senderHolder: CommandSenderHolder,
            commandName: String,
            completedArgs: List<String>,
            uncompletedArg: String
        ): List<String> {
            // TODO: Returns list of files in the EmbedScript/export
            return emptyList()
        }
    }

    private class ReloadHandler(
        val configuration: Configuration,
        val scriptManager: ScriptManager,
        val permissionDetector: PermissionDetector
    ) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(emptyList(), LastArgument.NotAllow)
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val sender = senderHolder.commandSender

            sender.sendMessage(Prefix.INFO + "Reloading configuration and scripts...")

            val cfgReloadResult = runCatching { configuration.reload() }
            cfgReloadResult.exceptionOrNull()?.let {
                sender.sendMessage(Prefix.ERROR + "Reload failed! (error: " + it.message + ")")
                it.printStackTrace()
                return
            }

            val scriptReloadResult = runCatching { scriptManager.reload() }
            scriptReloadResult.exceptionOrNull()?.let {
                sender.sendMessage(Prefix.ERROR + "Reload failed! (error: " + it.message + ")")
                it.printStackTrace()
                return
            }

            runCatching { permissionDetector.reload() }.exceptionOrNull()?.let {
                sender.sendMessage(Prefix.ERROR + "Reload failed! (error: ${it.message})")
                it.printStackTrace()
                return
            }

            sender.sendMessage(Prefix.SUCCESS + "Successfully reloaded!")
        }
    }

    private class TeleportHandler() : CommandHandler(
        ExecutionThreadType.SYNCHRONOUS,
        ArgumentInfoList(
            listOf(
                RequiedArgumentInfo("world"),
                RequiedArgumentInfo("x"),
                RequiedArgumentInfo("y"),
                RequiedArgumentInfo("z")
            ),
            LastArgument.NotAllow
        )
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val player = senderHolder.tryCastToPlayerOrMessage() ?: return

            val world = Bukkit.getWorld(args["world"]) ?: run {
                player.sendMessage(Prefix.ERROR + "World: ${args["world"]} not exists.")
                return
            }

            val playerLocation = player.location
            val x = args.getValue("x").toInt()
            val y = args.getValue("y").toInt()
            val z = args.getValue("z").toInt()
            player.teleport(
                Location(
                    world,
                    x + 0.5,
                    y.toDouble(),
                    z + 0.5,
                    playerLocation.yaw,
                    playerLocation.pitch
                )
            )

            player.sendMessage(Prefix.SUCCESS + "Teleported.")
        }
    }
}
