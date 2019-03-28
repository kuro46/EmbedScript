package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.Prefix
import com.github.kuro46.embedscript.util.MojangUtil
import com.github.kuro46.embedscript.util.Scheduler
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.md_5.bungee.api.chat.*
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors

/**
 * @author shirokuro
 */
class ScriptUI(private val scriptManager: ScriptManager) {
    private val pageManager: Cache<CommandSender, (Int) -> Unit> = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .weakKeys()
        .build()

    fun embed(sender: CommandSender,
              position: ScriptPosition,
              script: Script) {
        Objects.requireNonNull(script)

        if (scriptManager.contains(position)) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script already exists in that place.")
            return
        }

        scriptManager.put(position, script)

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully embedded.")
    }

    fun add(sender: CommandSender,
            position: ScriptPosition,
            script: Script) {
        Objects.requireNonNull(script)

        if (!scriptManager.contains(position)) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.")
            return
        }
        scriptManager.put(position, script)

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully added.")
    }

    fun remove(sender: CommandSender, position: ScriptPosition) {
        if (scriptManager.remove(position) == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.")
            return
        }

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully removed.")
    }

    fun view(sender: CommandSender, position: ScriptPosition) {
        if (!scriptManager.contains(position)) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.")
            return
        }
        val scripts = scriptManager[position]
        Scheduler.execute {
            val messages = ArrayList<Array<BaseComponent>>()
            for (script in scripts) {
                val authorId = script.author
                val player = Bukkit.getPlayer(authorId)
                val author = if (player != null) {
                    player.name
                } else {
                    val result = MojangUtil.getName(authorId)
                    when (result) {
                        is MojangUtil.FindNameResult.Found -> result.name
                        else -> {
                            sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to find user name")
                            return@execute
                        }
                    }
                }
                messages.add(TextComponent.fromLegacyText("author $author"))
                messages.add(TextComponent.fromLegacyText("@listen-move " + collectionToString(script.moveTypes)))
                messages.add(TextComponent.fromLegacyText("@listen-click " + collectionToString(script.clickTypes)))
                messages.add(TextComponent.fromLegacyText("@listen-push " + collectionToString(script.pushTypes)))
                val scriptMap = script.script
                for (key in scriptMap.keySet()) {
                    val value = scriptMap.get(key)
                    messages.add(TextComponent.fromLegacyText('@'.toString() + key + ' '.toString() + collectionToString(value)))
                }
            }
            sendPage("Script information", sender, messages, 0, 12)
        }
    }

    private fun collectionToString(collection: Collection<*>): String {
        return when {
            collection.isEmpty() -> "NONE"
            collection.size == 1 -> collection.iterator().next().toString()
            else -> collection.stream()
                .map { it.toString() }
                .map { s -> s + ChatColor.RESET }
                .collect(Collectors.joining("][", "[", "]"))
        }
    }

    /**
     * Send list of scripts to player
     *
     * @param player    Player
     * @param world     World (Nullable)
     * @param pageIndex page index
     */
    fun list(player: Player, world: String?, filter: Script?, pageIndex: Int) {
        Scheduler.execute {
            val messages = scriptManager.scripts.asMap().entries.stream()
                .filter { entry ->
                    world == null ||
                        world == "all" ||
                        world.equals(entry.key.world, ignoreCase = true)
                }
                .sorted(ScriptPositionComparator())
                .collect(ScriptCollector(filter))

            val target = if (world == null || world == "all")
                "this server"
            else
                world

            if (messages.isEmpty()) {
                player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in " + target)
            } else {
                sendPage("List of scripts in $target",
                    player,
                    messages,
                    pageIndex)
            }
        }
    }

    fun changePage(player: Player, pageIndex: Int) {
        val consumer = pageManager.getIfPresent(player)
        if (consumer == null) {
            player.sendMessage(Prefix.ERROR_PREFIX + "Cannot get your page.")
            return
        }
        consumer(pageIndex)
    }

    private fun sendPage(title: String,
                         sender: CommandSender,
                         messages: Collection<Array<BaseComponent>>,
                         pageIndex: Int,
                         chatHeight: Int = UNFOCUSED_CHAT_HEIGHT) {
        val availableMessageHeight = chatHeight - 3
        val pages = splitMessages(messages, availableMessageHeight)

        if (pageIndex >= pages.size || pageIndex < 0) {
            sender.sendMessage("Out of bounds")
            return
        }
        val page = pages[pageIndex]

        val separator = titleToSeparator(title)
        sender.sendMessage(separator)
        page.forEach { baseComponents -> sender.spigot().sendMessage(*baseComponents) }

        val previousPageIndex = if (pageIndex - 1 < 0) pages.size - 1 else pageIndex - 1
        val nextPageIndex = if (pageIndex + 1 >= pages.size) 0 else pageIndex + 1

        sender.spigot().sendMessage(*ComponentBuilder("")
            .append(ComponentBuilder("<<Previous>>")
                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/embedscript page $previousPageIndex"))
                .create())
            .append("   ")
            .append(ComponentBuilder("<<Next>>")
                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/embedscript page $nextPageIndex"))
                .create())
            .append("   ")
            .append(ComponentBuilder("<<Page ${pageIndex + 1} of ${pages.size}>>")
                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, ""))
                .create())
            .create())
        sender.sendMessage(separator)

        pageManager.put(sender) { value -> sendPage(title, sender, messages, value, chatHeight) }
    }

    private fun titleToSeparator(title: String): String {
        var decorated = title
        decorated = "---< $decorated >---"
        val separatorString = StringUtils.repeat("-", (CHAT_WIDTH - decorated.length) / 2)
        return separatorString + decorated + separatorString
    }

    private fun splitMessages(messages: Collection<Array<BaseComponent>>, maximumLines: Int): List<List<Array<BaseComponent>>> {
        val pages = ArrayList<List<Array<BaseComponent>>>()
        val buffer = ArrayList<Array<BaseComponent>>()
        for (message in messages) {
            buffer.add(message)
            if (buffer.size >= maximumLines) {
                pages.add(ArrayList(buffer))
                buffer.clear()
            }
        }
        if (!buffer.isEmpty()) {
            val lastPage = ArrayList(buffer)
            //last page pad with space
            val padLines = maximumLines - lastPage.size
            for (i in 0 until padLines) {
                lastPage.add(TextComponent.fromLegacyText(""))
            }
            pages.add(lastPage)
        }
        return pages
    }

    private class ScriptPositionComparator : Comparator<MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>>, Serializable {
        override fun compare(entry: MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>,
                             entry1: MutableMap.MutableEntry<ScriptPosition, MutableCollection<Script>>): Int {
            val position = entry.key
            val position1 = entry1.key

            val worldCompareTo = position.world.compareTo(position1.world)
            if (worldCompareTo != 0) {
                return worldCompareTo
            }
            val yCompareTo = Integer.compare(position.y, position1.y)
            if (yCompareTo != 0) {
                return yCompareTo
            }
            val xCompareTo = Integer.compare(position.x, position1.x)
            return if (xCompareTo != 0) {
                xCompareTo
            } else Integer.compare(position.z, position1.z)

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
                val scripts = entry.value

                for (script in scripts) {
                    if (filter != null && isFilterable(script, filter)) {
                        continue
                    }

                    val tpCommand = ("/embedscript teleport " + position.world + " " + position.x + " "
                        + position.y + " " + position.z)
                    val message = ComponentBuilder("")
                        .append("[" + (messages.size + 1) + "] ")
                        .append("World: " + position.world + " X: " + position.x
                            + " Y: " + position.y + " Z: " + position.z + " (click here)")
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tpCommand)))
                        .create()
                    messages.add(message)
                }
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

        override fun finisher(): Function<MutableCollection<Array<BaseComponent>>, MutableCollection<Array<BaseComponent>>> {
            return Function { message -> message }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return emptySet()
        }

        private fun isFilterable(script: Script, filter: Script): Boolean {
            if (isFilterable<Script.MoveType>(script.moveTypes, filter.moveTypes)) {
                return true
            }
            if (isFilterable<Script.ClickType>(script.clickTypes, filter.clickTypes)) {
                return true
            }
            if (isFilterable<Script.PushType>(script.pushTypes, filter.pushTypes)) {
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

    companion object {
        private const val UNFOCUSED_CHAT_HEIGHT = 10
        private const val CHAT_WIDTH = 50
    }
}
