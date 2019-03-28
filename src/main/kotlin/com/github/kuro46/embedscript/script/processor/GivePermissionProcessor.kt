package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.Configuration
import com.github.kuro46.embedscript.script.ParseException
import com.google.common.collect.ImmutableList
import org.apache.commons.lang.ArrayUtils
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.Plugin
import java.util.*

class GivePermissionProcessor(plugin: Plugin, configuration: Configuration) : Processor {
    override val executor: Processor.Executor
    override val parser: Processor.Parser

    override val key: String
        get() = "give-permission"

    override val omittedKey: String
        get() = "gp"

    init {
        this.executor = GivePermissionExecutor(plugin)
        this.parser = GivePermissionParser(configuration)
    }

    private class GivePermissionExecutor(private val plugin: Plugin) : AbstractExecutor() {
        private val attachments = HashMap<Player, PermissionAttachment>()

        override fun prepareExecute(trigger: Player, matchedValues: ImmutableList<String>) {
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

        override fun endExecute(trigger: Player, matchedValues: ImmutableList<String>) {
            val attachment = attachments[trigger]
            attachment?.remove()
        }
    }

    private class GivePermissionParser(private val configuration: Configuration) : AbstractParser() {

        @Throws(ParseException::class)
        override fun build(builder: ScriptBuilder, key: String, matchedValues: ImmutableList<String>) {
            if (!matchedValues.isEmpty()) {
                builder.script.putAll(key, matchedValues)
            } else {
                val preferPermissions = HashSet<String>()
                val permissionsForActions = configuration.permissionsForActions
                for (action in builder.script.values()) {
                    var permissionsForAction: List<String>? = permissionsForActions!![action]

                    if (permissionsForAction == null) {
                        var skipElement = 1
                        while (permissionsForAction == null) {
                            val split = action.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            ArrayUtils.reverse(split)
                            val skipped = Arrays.stream(split)
                                .skip(skipElement.toLong())
                                .toArray()
                            if (skipped.isEmpty()) {
                                break
                            }
                            ArrayUtils.reverse(skipped)
                            permissionsForAction = permissionsForActions[skipped.joinToString(" ")]

                            skipElement++
                        }
                    }

                    if (permissionsForAction == null) {
                        continue
                    }
                    preferPermissions.addAll(permissionsForAction)
                }
                builder.script.putAll(key, preferPermissions)
            }
        }
    }
}
