package com.github.kuro46.embedscript.util

import java.util.regex.Pattern

/**
 * @author shirokuro
 */
object Utils {
    private val PATTERN_SPLIT_BY_FIRST_SPACE = Pattern.compile("(?<left>[^ ]+) (?<right>.+)")

    fun splitByFirstSpace(string: String): Pair<String, String>? {
        val splitMatcher = PATTERN_SPLIT_BY_FIRST_SPACE.matcher(string)
        return if (!splitMatcher.find()) {
            null
        } else Pair(splitMatcher.group("left"), splitMatcher.group("right"))
    }
}
