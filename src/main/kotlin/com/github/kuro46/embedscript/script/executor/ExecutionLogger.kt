package com.github.kuro46.embedscript.script.executor

import com.github.kuro46.embedscript.LogConfiguration
import com.github.kuro46.embedscript.script.Script
import com.github.kuro46.embedscript.script.ScriptPosition
import com.github.kuro46.embedscript.script.ScriptUtils
import com.github.kuro46.embedscript.util.PlaceholderData
import com.github.kuro46.embedscript.util.Replacer
import java.util.StringJoiner
import java.util.logging.Logger
import org.bukkit.entity.Player

/**
 * @author shirokuro
 */
class ExecutionLogger(val logger: Logger, val configuration: LogConfiguration) {
    val replacer = Replacer<LogData>()

    init {
        replacer.add(PlaceholderData("<trigger>") { it.player.name })
        replacer.add(PlaceholderData("<trigger_world>") { it.player.world.name })
        replacer.add(PlaceholderData("<trigger_x>") { it.player.location.blockX })
        replacer.add(PlaceholderData("<trigger_y>") { it.player.location.blockY })
        replacer.add(PlaceholderData("<trigger_z>") { it.player.location.blockZ })
        replacer.add(PlaceholderData("<script_world>") { it.scriptPosition.world })
        replacer.add(PlaceholderData("<script_x>") { it.scriptPosition.x })
        replacer.add(PlaceholderData("<script_y>") { it.scriptPosition.y })
        replacer.add(PlaceholderData("<script_z>") { it.scriptPosition.z })
        replacer.add(
            PlaceholderData("<script>") {
                val script = it.script
                val joiner = StringJoiner(" ")
                for (parentKeyData in script.keys) {
                    for ((key, values) in parentKeyData) {
                        joiner.add("@$key ${ScriptUtils.toString(values)}")
                    }
                }
                joiner.toString()
            }
        )
    }

    fun log(logData: LogData) {
        if (!configuration.enabled) {
            return
        }

        logger.info(replacer.execute(configuration.format, logData))
    }
}

data class LogData(
    val player: Player,
    val scriptPosition: ScriptPosition,
    val script: Script
)
