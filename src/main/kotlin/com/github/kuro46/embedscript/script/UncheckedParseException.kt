package com.github.kuro46.embedscript.script

class UncheckedParseException(cause: ParseException) : RuntimeException(cause) {
    @Synchronized
    fun getCause(): ParseException {
        return super.cause as ParseException
    }
}
