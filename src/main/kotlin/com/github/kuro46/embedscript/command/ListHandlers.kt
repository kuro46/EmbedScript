package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.util.PageUtils
import com.github.kuro46.embedscript.util.command.ArgumentInfoList
import com.github.kuro46.embedscript.util.command.CommandHandler
import com.github.kuro46.embedscript.util.command.CommandSenderHolder
import com.github.kuro46.embedscript.util.command.ExecutionThreadType
import com.github.kuro46.embedscript.util.command.OptionalArgumentInfo
import com.github.kuro46.embedscript.util.command.OptionalArguments
import java.io.Serializable
import java.util.ArrayList
import java.util.Comparator
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function as JavaFunction
import java.util.function.Supplier
import java.util.stream.Collector
import kotlin.streams.toList
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author shirokuro
 */
object ListHandlers {
    class ListHandler(
        private val scriptManager: ScriptManager
    ) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            OptionalArguments(
                listOf(
                    OptionalArgumentInfo("world", null),
                    OptionalArgumentInfo("pageNumber", "1")
                )
            )
        ),
        "Displays list of scripts in the [world] or current world."
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val player = senderHolder.tryCastToPlayerOrMessage() ?: return
            val world = args.getOrElse("world") { player.world.name }
            val pageNumber = args.getValue("pageNumber").toInt()
            val scope = ListScope.World(world)
            list(scriptManager, player, scope, null, pageNumber - 1)
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

    class ListAllHandler(
        private val scriptManager: ScriptManager
    ) : CommandHandler(
        ExecutionThreadType.ASYNCHRONOUS,
        ArgumentInfoList(
            emptyList(),
            OptionalArguments(
                listOf(
                    OptionalArgumentInfo("pageNumber", "1")
                )
            )
        ),
        "Displays list of scripts in this server."
    ) {
        override fun handleCommand(
            senderHolder: CommandSenderHolder,
            args: Map<String, String>
        ) {
            val player = senderHolder.tryCastToPlayerOrMessage() ?: return
            val pageNumber = args.getValue("pageNumber").toInt()
            val scope = ListScope.Server
            list(scriptManager, player, scope, null, pageNumber - 1)
        }
    }

    fun list(
        scriptManager: ScriptManager,
        player: Player,
        scope: ListScope,
        filter: Script?,
        pageIndex: Int
    ) {
        val messages =
            scriptManager.getScripts().entries.stream()
                .filter { entry ->
                    when (scope) {
                        is ListScope.Server -> true
                        is ListScope.World -> scope.name.equals(entry.key.world, true)
                    }
                }
                .sorted(ScriptPositionComparator())
                .collect(ScriptCollector(filter))

        val target = if (scope is ListScope.World) scope.name else "this server"

        if (messages.isEmpty()) {
            player.sendMessage(Prefix.ERROR + "Script not exists in $target")
        } else {
            val command = if (scope is ListScope.World) {
                "list ${scope.name}"
            } else {
                "listAll"
            }
            PageUtils.sendPage(
                "List of scripts in $target",
                player,
                messages,
                pageIndex
            ) { index ->
                val pageNumber = index + 1
                "/embedscript $command $pageNumber"
            }
        }
    }

    sealed class ListScope {
        object Server : ListScope()
        data class World(val name: String) : ListScope()
    }

    private class ScriptPositionComparator :
        Comparator<Map.Entry<ScriptPosition, Collection<Script>>>,
        Serializable {
        override fun compare(
            entry: Map.Entry<ScriptPosition, Collection<Script>>,
            entry1: Map.Entry<ScriptPosition, Collection<Script>>
        ): Int {
            val position = entry.key
            val position1 = entry1.key

            position.world.compareTo(position1.world).let { if (it != 0) return it }
            position.y.compareTo(position1.y).let { if (it != 0) return it }
            position.x.compareTo(position1.x).let { if (it != 0) return it }
            return position.z.compareTo(position1.z)
        }
    }

    private class ScriptCollector(private val filter: Script?) :
        Collector<Map.Entry<ScriptPosition, Collection<Script>>,
            MutableCollection<Array<BaseComponent>>,
            Collection<Array<BaseComponent>>> {

        override fun supplier(): Supplier<MutableCollection<Array<BaseComponent>>> {
            return Supplier { ArrayList<Array<BaseComponent>>() }
        }

        override fun accumulator(): BiConsumer<MutableCollection<Array<BaseComponent>>, Map.Entry<ScriptPosition, Collection<Script>>> {
            return BiConsumer { messages, entry ->
                val position = entry.key
                val scripts = ArrayList(entry.value).filter { filter == null || isFilterable(it, filter) }

                if (scripts.isEmpty()) {
                    return@BiConsumer
                }

                val viewCommand = "/embedscript viewat ${position.world} ${position.x} ${position.y} ${position.z}"
                val tpCommand = "/embedscript teleport ${position.world} ${position.x} ${position.y} ${position.z}"
                val mainMessage =
                    "[${messages.size + 1}] ${position.world}, ${position.x}, " +
                        "${position.y}, ${position.z} "
                val message =
                    ComponentBuilder(mainMessage)
                        .append(
                            ComponentBuilder("[detail]")
                                .event(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        TextComponent.fromLegacyText(viewCommand)
                                    )
                                )
                                .event(
                                    ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        viewCommand
                                    )
                                )
                                .create()
                        )
                        .append(" ")
                        .append(
                            ComponentBuilder("[teleport]")
                                .event(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        TextComponent.fromLegacyText(tpCommand)
                                    )
                                )
                                .event(
                                    ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        tpCommand
                                    )
                                )
                                .create()
                        )
                        .create()
                messages.add(message)
            }
        }

        override fun combiner(): BinaryOperator<MutableCollection<Array<BaseComponent>>> {
            return BinaryOperator { messages, messages1 ->
                val result = ArrayList<Array<BaseComponent>>()
                result.addAll(messages)
                result.addAll(messages1)
                result
            }
        }

        override fun finisher(): JavaFunction<MutableCollection<Array<BaseComponent>>, Collection<Array<BaseComponent>>> {
            return JavaFunction { message -> message }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return emptySet()
        }

        private fun isFilterable(targetScript: Script, targetFilter: Script): Boolean {
            val firstCheck =
                isFilterable(targetScript.clickTypes, targetFilter.clickTypes) ||
                    isFilterable(targetScript.moveTypes, targetFilter.moveTypes) ||
                    isFilterable(targetScript.pushTypes, targetFilter.pushTypes)
            if (firstCheck) {
                return true
            }

            for (filterParentOption in targetFilter.keys) {
                for (targetParentOption in targetScript.keys) {
                    if (isFilterable(filterParentOption.values, targetParentOption.values)) {
                        return true
                    }

                    for (filterChildOption in filterParentOption.children) {
                        for (targetChildOption in targetParentOption.children) {
                            if (isFilterable(filterChildOption.values, targetChildOption.values)) {
                                return true
                            }
                        }
                    }
                }
            }

            return false
        }

        private fun <E> isFilterable(target: Collection<E>, filter: Collection<E>): Boolean {
            for (f in filter) {
                if (target.isEmpty()) {
                    return true
                }
                for (t in target) {
                    if (f != t) {
                        return true
                    }
                }
            }

            // All filter's entries are exist in target
            return false
        }
    }
}
