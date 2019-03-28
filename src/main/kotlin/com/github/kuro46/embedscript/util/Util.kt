package com.github.kuro46.embedscript.util

import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.Pair

/**
 * @author shirokuro
 */
class Util private constructor() {

    init {
        throw UnsupportedOperationException("Utility class")
    }

    private class Patterns internal constructor(val pattern: Pattern, val escapedPattern: Pattern)

    companion object {
        private val PATTERN_SPLIT_BY_FIRST_SPACE = Pattern.compile("(?<left>[^ ]+) (?<right>.+)")

        fun splitByFirstSpace(string: String): Pair<String, String>? {
            val splitMatcher = PATTERN_SPLIT_BY_FIRST_SPACE.matcher(string)
            return if (!splitMatcher.find()) {
                null
            } else Pair(splitMatcher.group("left"), splitMatcher.group("right"))

        }

        fun joinStringSpaceDelimiter(startIndex: Int, strings: Array<String>): String {
            return Arrays.stream(strings)
                .skip(startIndex.toLong())
                .collect(Collectors.joining(" "))
        }

        fun replaceAndUnescape(source: String, target: String, replacement: String): String {
            return replaceAndUnescape(source, target, target, replacement, true)
        }

        fun replaceAndUnescape(source: String, target: String, escapeTo: String, replacement: String, quote: Boolean): String {
            var source = source
            val patterns = createPatterns(target, quote)
            val pattern = patterns.pattern
            val escapedPattern = patterns.escapedPattern

            source = pattern.matcher(source).replaceAll("$1$replacement")
            source = escapedPattern.matcher(source).replaceAll(escapeTo)

            return source
        }

        fun splitAndUnescape(source: String, target: String): List<String> {
            var source = source
            val patterns = createPatterns(target, true)
            val pattern = patterns.pattern
            val escapedPattern = patterns.escapedPattern

            val quoteReplacementTarget = Matcher.quoteReplacement(target)
            source = pattern.matcher(source).replaceAll("$1 $quoteReplacementTarget")
            return Arrays.stream(pattern.split(source))
                .map { s -> escapedPattern.matcher(s).replaceAll(quoteReplacementTarget) }
                .collect(Collectors.toList())
        }

        private fun createPatterns(target: String, quote: Boolean): Patterns {
            var target = target
            if (quote) {
                target = Pattern.quote(target)
            }
            val pattern = Pattern.compile("(^|[^\\\\])$target")
            val escapedPattern = Pattern.compile("\\\\" + target)

            return Patterns(pattern, escapedPattern)
        }
    }
}
