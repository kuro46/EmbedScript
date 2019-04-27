package com.github.kuro46.embedscript.command

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptManager
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtil
import com.github.kuro46.embedscript.script.processor.ScriptProcessor
import com.github.kuro46.embedscript.util.PageUtil
import com.github.kuro46.embedscript.util.command.Arguments
import com.github.kuro46.embedscript.util.command.CommandHandler
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.Serializable
import java.util.ArrayList
import java.util.Comparator
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Supplier
import java.util.stream.Collector
import kotlin.streams.toList

object ListHandlers {
    class ListHandler(private val presetName: String?,
                      private val scriptProcessor: ScriptProcessor,
                      private val scriptManager: ScriptManager) : CommandHandler(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            val player = sender as Player
            val world = args.getOrElse(0) { player.world.name }
            val pageNumber = args.getInt(sender, 1, 1) ?: return true
            val filter = presetName?.let {
                scriptProcessor.parse(player.uniqueId, "@preset " + ScriptUtil.toString(it))
            }
            val scope = ListScope.World(world)
            list(scriptManager, player, scope, filter, pageNumber - 1)
            return true
        }

        override fun onTabComplete(sender: CommandSender, uncompletedArg: String, uncompletedArgIndex: Int, completedArgs: Arguments): List<String> {
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

    class ListAllHandler(private val presetName: String?,
                         private val scriptProcessor: ScriptProcessor,
                         private val scriptManager: ScriptManager) : CommandHandler(SenderType.Player()) {
        override fun onCommand(sender: CommandSender, command: String, args: Arguments): Boolean {
            val player = sender as Player
            val pageNumber = args.getInt(sender, 0, 1) ?: return true
            val filter = presetName?.let {
                scriptProcessor.parse(player.uniqueId, "@preset " + ScriptUtil.toString(it))
            }
            val scope = ListScope.Server
            list(scriptManager, player, scope, filter, pageNumber - 1)
            return true
        }
    }

    fun list(scriptManager: ScriptManager, player: Player, scope: ListScope, filter: Script?, pageIndex: Int) {
        val messages = scriptManager.scripts.asMap().entries.stream()
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
            player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in $target")
        } else {
            val command = if (scope is ListScope.World) {
                "list ${scope.name}"
            } else {
                "listAll"
            }
            PageUtil.sendPage("List of scripts in $target",
                    player,
                    messages,
                    pageIndex) { index ->
                val pageNumber = index + 1
                "/embedscript $command $pageNumber"
            }
        }
    }

    sealed class ListScope {
        object Server : ListScope()
        data class World(val name: String) : ListScope()
    }

    private class ScriptPositionComparator : Comparator<MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>>, Serializable {
        override fun compare(entry: MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>,
                             entry1: MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>): Int {
            val position = entry.key
            val position1 = entry1.key

            position.world.compareTo(position1.world).let { if (it != 0) return it }
            position.y.compareTo(position1.y).let { if (it != 0) return it }
            position.x.compareTo(position1.x).let { if (it != 0) return it }
            return position.z.compareTo(position1.z)
        }
    }

    private class ScriptCollector(private val filter: Script?)
        : Collector<MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>,
            MutableCollection<Array<BaseComponent>>,
            MutableCollection<Array<BaseComponent>>> {

        override fun supplier(): Supplier<MutableCollection<Array<BaseComponent>>> {
            return Supplier { ArrayList<Array<BaseComponent>>() }
        }

        override fun accumulator(): BiConsumer<MutableCollection<Array<BaseComponent>>, MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>> {
            return BiConsumer { messages, entry ->
                val position = entry.key
                val scripts = ArrayList(entry.value).filter { filter == null || isFilterable(it, filter) }

                if (scripts.isEmpty()) {
                    return@BiConsumer
                }

                val tpCommand = "/embedscript view ${position.world} ${position.x} ${position.y} ${position.z}"
                val message = ComponentBuilder("")
                        .append("[${messages.size + 1}] ")
                        .append("World: ${position.world} X: ${position.x} " +
                                "Y: ${position.y} Z: ${position.z} (click to details)")
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tpCommand)))
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

        override fun finisher(): java.util.function.Function<MutableCollection<Array<BaseComponent>>, MutableCollection<Array<BaseComponent>>> {
            return java.util.function.Function { message -> message }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return emptySet()
        }

        private fun isFilterable(script: Script, filter: Script): Boolean {
            if (isFilterable(script.moveTypes, filter.moveTypes)) {
                return true
            }
            if (isFilterable(script.clickTypes, filter.clickTypes)) {
                return true
            }
            if (isFilterable(script.pushTypes, filter.pushTypes)) {
                return true
            }

            val scriptMap = script.script
            for (key in scriptMap.keySet()) {
                val scriptValues = scriptMap.get(key)
                val filterMap = filter.script
                val filterValues = filterMap.get(key)

                if (isFilterable(scriptValues, filterValues)) {
                    return true
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
            return false
        }
    }

}
