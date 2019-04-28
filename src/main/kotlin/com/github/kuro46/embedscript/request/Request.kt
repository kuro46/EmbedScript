package com.github.kuro46.embedscript.request

import com.github.kuro46.embedscript.script.Script

/**
 * @author shirokuro
 */
sealed class Request {
    data class Embed(val script: Script) : Request()
    data class Add(val script: Script) : Request()
    object Remove : Request()
    object View : Request()
}
