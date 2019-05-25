package com.github.kuro46.embedscript.util

import java.util.regex.Pattern

/**
 * @author shirokuro
 */
object Util {
    private val PATTERN_SPLIT_BY_FIRST_SPACE = Pattern.compile("(?<left>[^ ]+) (?<right>.+)")

    private class Patterns(val pattern: Pattern, val escapedPattern: Pattern)

    fun splitByFirstSpace(string: String): Pair<String, String>? {
        val splitMatcher = PATTERN_SPLIT_BY_FIRST_SPACE.matcher(string)
        return if (!splitMatcher.find()) {
            null
        } else Pair(splitMatcher.group("left"), splitMatcher.group("right"))

    }

    fun replaceAndUnescape(source: String, target: String, replacement: String): String {
        return replaceAndUnescape(source, target, target, replacement, true)
    }

    fun replaceAndUnescape(
            source: String,
            target: String,
            escapeTo: String,
            replacement: String,
            quote: Boolean
    ): String {
        @Suppress("NAME_SHADOWING")
        var source = source
        val patterns = createPatterns(target, quote)
        val pattern = patterns.pattern
        val escapedPattern = patterns.escapedPattern

        source = pattern.matcher(source).replaceAll("$1$replacement")
        source = escapedPattern.matcher(source).replaceAll(escapeTo)

        return source
    }

    private fun createPatterns(target: String, quote: Boolean): Patterns {
        @Suppress("NAME_SHADOWING")
        var target = target
        if (quote) {
            target = Pattern.quote(target)
        }
        val pattern = Pattern.compile("(^|[^\\\\])$target")
        val escapedPattern = Pattern.compile("\\\\" + target)

        return Patterns(pattern, escapedPattern)
    }
}
