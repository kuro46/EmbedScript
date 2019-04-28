package com.github.kuro46.embedscript.script

/**
 * @author shirokuro
 */
class ParseException : Exception {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}
