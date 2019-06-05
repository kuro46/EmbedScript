package com.github.kuro46.embedscript.util

import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author shirokuro
 */
class Replacer<T> : CopyOnWriteArrayList<PlaceholderData<T>>() {
    fun execute(input: String, target: T): String {
        var replaced: String = input
        for ((placeholder, valueFactory) in this) {
            replaced = replaced.replace(placeholder, valueFactory(target).toString())
        }
        return replaced
    }
}

typealias ValueFactory <T> = (T) -> Any

data class PlaceholderData<T>(val placeholder: String, val valueFactory: ValueFactory<T>)
