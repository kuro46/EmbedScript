package com.github.kuro46.embedscript.util

import com.github.kuro46.embedscript.Prefix
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import org.apache.commons.lang.StringUtils
import org.bukkit.command.CommandSender
import java.util.ArrayList

/**
 * @author shirokuro
 */
object PageUtil {
    // upper title, under title and navigation bar
    private const val UI_LINES = 3
    private const val UNFOCUSED_CHAT_HEIGHT = 10
    private const val CHAT_WIDTH = 50

    fun sendPage(
            title: String,
            sender: CommandSender,
            messages: Collection<Array<BaseComponent>>,
            pageIndex: Int,
            chatHeight: Int = UNFOCUSED_CHAT_HEIGHT,
            commandGenerator: (Int) -> String
    ) {
        val availableMessageHeight = chatHeight - UI_LINES
        val pages = splitMessages(messages, availableMessageHeight)

        if (pageIndex >= pages.size || pageIndex < 0) {
            sender.sendMessage(Prefix.ERROR + "Page index out of bounds")
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
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, commandGenerator(previousPageIndex)))
                        .create())
                .append("   ")
                .append(ComponentBuilder("<<Next>>")
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, commandGenerator(nextPageIndex)))
                        .create())
                .append("   ")
                .append(ComponentBuilder("<<Page ${pageIndex + 1} of ${pages.size}>>")
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, ""))
                        .create())
                .create())
        sender.sendMessage(separator)
    }

    private fun titleToSeparator(title: String): String {
        var decorated = title
        decorated = "---< $decorated >---"
        val separatorString = StringUtils.repeat("-", (CHAT_WIDTH - decorated.length) / 2)
        return separatorString + decorated + separatorString
    }

    private fun splitMessages(
            messages: Collection<Array<BaseComponent>>,
            maximumLines: Int
    ): List<List<Array<BaseComponent>>> {
        val pages: MutableList<List<Array<BaseComponent>>> = ArrayList()
        val buffer: MutableList<Array<BaseComponent>> = ArrayList()
        for (message in messages) {
            buffer.add(message)
            if (buffer.size >= maximumLines) {
                pages.add(ArrayList(buffer))
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) {
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
}
