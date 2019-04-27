package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.processor.executor.AbstractExecutor
import com.github.kuro46.embedscript.script.processor.executor.ExecutionMode
import com.github.kuro46.embedscript.script.processor.parser.AbstractParser
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.Plugin
import java.util.HashMap

class GivePermissionProcessor {
    companion object {
        fun register(processor: ScriptProcessor) {
            processor.registerProcessor(ChildProcessor(
                    key = "give-permission",
                    omittedKey = "gp",
                    executor = GivePermissionExecutor(processor.plugin),
                    parser = GivePermissionParser()
            ))
        }
    }

    private class GivePermissionExecutor(private val plugin: Plugin) : AbstractExecutor() {
        private val attachments = HashMap<Player, PermissionAttachment>()

        override val executionMode: ExecutionMode
            get() {
                return if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms"))
                    ExecutionMode.ASYNCHRONOUS
                else ExecutionMode.SYNCHRONOUS
            }

        override fun prepareExecute(trigger: Player, matchedValues: List<String>) {
            val attachment = if (matchedValues.isEmpty()) null else trigger.addAttachment(plugin)
            for (matchedValue in matchedValues) {
                if (trigger.hasPermission(matchedValue)) {
                    continue
                }
                attachment!!.setPermission(matchedValue, true)
            }

            if (attachment != null) {
                attachments[trigger] = attachment
            }
        }

        override fun endExecute(trigger: Player, matchedValues: List<String>) {
            attachments[trigger]?.remove()
        }
    }

    private class GivePermissionParser : AbstractParser() {
        override fun build(builder: ScriptBuilder, key: String, matchedValues: List<String>) {
            builder.script.putAll(key, matchedValues)
        }

        override fun getSuggestions(uncompletedArg: String): List<String> {
            return Bukkit.getPluginManager().permissions.map { it.name }
        }
    }
}
