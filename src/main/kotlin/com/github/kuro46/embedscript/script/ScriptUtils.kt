package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.script.executor.ScriptProcessor
import com.github.kuro46.embedscript.util.Utils
import java.util.Locale
import java.util.regex.Pattern

/**
 * @author shirokuro
 */
object ScriptUtils {

    fun toString(values: Iterable<String>): String {
        return values.joinToString("][", "[", "]")
    }

    fun toString(value: String): String {
        return "[$value]"
    }

    /**
     * Convert from legacy(ScriptBlock) format(e.g. '@command /cmd arg')<br></br>
     *
     * @param author author of this script
     * @param legacy legacy format of script
     * @return script
     */
    fun createScriptFromLegacyFormat(
        processor: ScriptProcessor,
        author: Author,
        eventType: EventType,
        legacy: String
    ): List<Script> {
        /*
         * Targets
         * @bypassperm:permission action
         * @command action
         * @player action
         * @bypass action
         */

        @Suppress("NAME_SHADOWING")
        val legacy = if (legacy.startsWith(']') && legacy.endsWith('[')) {
            legacy
        } else {
            "[$legacy]"
        }

        fun parseSingleScript(stringScript: String): Script {
            val pair = Utils.splitByFirstSpace(stringScript) ?: throw ParseException("Illegal script")
            val key = pair.first
            val value = "[${pair.second}]"

            val formatBuilder = LinkedHashMap<String, String>()

            formatBuilder["@preset"] = "[${eventType.presetName}]"

            when (key.toLowerCase(Locale.ENGLISH)) {
                "@command" -> formatBuilder["@cmd"] = value
                "@player" -> formatBuilder["@say"] = value
                "@bypass" -> formatBuilder["@preset"] = "[alternative-bypass]"
                else -> {
                    val bypassPermPattern = Pattern.compile("^@bypassperm:(.+)", Pattern.CASE_INSENSITIVE)
                    val bypassPermPatternMatcher = bypassPermPattern.matcher(key)

                    if (bypassPermPatternMatcher.find()) {
                        formatBuilder["@cmd"] = value
                        formatBuilder["@cmd.bypass"] = "[${bypassPermPatternMatcher.group(1)}]"
                    } else {
                        throw ParseException("'$key' is unsupported action type!")
                    }
                }
            }

            val formattedByNewVersion = StringBuilder()
            for ((key1, value1) in formatBuilder) {
                formattedByNewVersion.append(key1).append(" ").append(value1).append(" ")
            }
            // trim a space character at end of string
            val substring = formattedByNewVersion.substring(0, formattedByNewVersion.length - 1)

            return processor.parse(-1, author, substring)
        }

        val pattern = Pattern.compile("\\[([^\\[\\]]+)\\]")
        val matcher = pattern.matcher(legacy)
        val scripts = ArrayList<Script>()
        while (matcher.find()) {
            scripts.add(parseSingleScript(matcher.group(1)))
        }

        return scripts
    }
}
