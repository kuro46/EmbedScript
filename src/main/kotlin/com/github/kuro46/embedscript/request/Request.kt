package com.github.kuro46.embedscript.request

import com.github.kuro46.embedscript.script.Script

/**
 * @author shirokuro
 */
sealed class Request {
    data class Embed(val scripts: List<Script>) : Request() {
        constructor(script: Script) : this(listOf(script))
    }
    data class Add(val scripts: List<Script>) : Request() {
        constructor(script: Script) : this(listOf(script))
    }
    object Remove : Request()
    object View : Request()
}
