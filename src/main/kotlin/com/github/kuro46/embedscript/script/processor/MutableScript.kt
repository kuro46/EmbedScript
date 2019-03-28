package com.github.kuro46.embedscript.script.processor

import com.github.kuro46.embedscript.script.ParseException
import com.github.kuro46.embedscript.util.Pair
import com.github.kuro46.embedscript.util.Util
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import java.util.*

class MutableScript @Throws(ParseException::class)
constructor(private var script: String) {
    private val multimap = ArrayListMultimap.create<String, String>()
    private var view: ImmutableListMultimap<String, String>? = null

    init {
        script = escapeAtInSquareBrackets(script)
        val keyValueStrings = Util.splitAndUnescape(script, "@")

        for (keyValueString in keyValueStrings) {
            val trimmedKeyValue = keyValueString.trim { it <= ' ' }
            if (trimmedKeyValue.isEmpty()) {
                continue
            }
            val pair = splitToKeyValue(trimmedKeyValue)
            for (value in pair.value) {
                add(pair.key, value)
            }
        }
    }

    private fun escapeAtInSquareBrackets(string: String): String {
        val result = StringBuilder(string.length)
        var inSquareBrackets = false
        for (c in string.toCharArray()) {
            if (c == '[') {
                inSquareBrackets = true
            } else if (c == ']') {
                inSquareBrackets = false
            }

            if (inSquareBrackets && c == '@') {
                result.append("\\@")
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }

    fun clear() {
        multimap.clear()
        invalidateView()
    }

    fun add(key: String, value: String) {
        multimap.put(key, value)
        invalidateView()
    }

    fun replaceKey(value: String, oldKey: String, newKey: String) {
        while (multimap.remove(oldKey, value)) {
            multimap.put(newKey, value)
            invalidateView()
        }
    }

    fun putAllMapBehavior(other: MutableScript) {
        for (key in other.multimap.keySet()) {
            this.multimap.removeAll(key)
            this.multimap.putAll(key, other.multimap.get(key))
            invalidateView()
        }
    }

    private fun invalidateView() {
        view = null
    }

    fun getView(): ImmutableListMultimap<String, String> {
        if (view == null) {
            view = ImmutableListMultimap.copyOf(multimap)
        }
        return view!!
    }

    /**
     * Split string to KeyValue
     *
     * @param string expects "key [value1][value2]", "key [value]" or "key value"
     * @return KeyValue
     */
    @Throws(ParseException::class)
    private fun splitToKeyValue(string: String): Pair<String, List<String>> {
        val pair = Util.splitByFirstSpace(string) ?: throw ParseException("Failed to parse '$string' to KeyValue")
        // expect "key"
        val key = pair.key.toLowerCase(Locale.ENGLISH)
        // expect "[value]", or "[value1][value2]"
        val value = pair.value

        val values = splitValue(value)

        return Pair(key, values)
    }

    @Throws(ParseException::class)
    private fun splitValue(string: String): List<String> {
        var modifiableString = string
        if (modifiableString.isEmpty()) {
            return emptyList()
        } else if (!(modifiableString.startsWith("[") && modifiableString.endsWith("]"))) {
            throw ParseException("Value of the script is needed to starts with '[' and ends with ']' : $modifiableString")
        }

        // trim "[" and "]"
        modifiableString = modifiableString.substring(1, modifiableString.length - 1)

        // translate color codes
        modifiableString = Util.replaceAndUnescape(modifiableString, "&(?<code>[0123456789AaBbCcDdEeFfKkLlMmNnOoRr])",
            "&\${code}",
            "§\${code}",
            false)

        return Util.splitAndUnescape(modifiableString, "][")
    }
}
