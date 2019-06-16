package com.github.kuro46.embedscript.util.command

/**
 * List of ArgumentInfo.
 *
 * @constructor
 *
 * @param requied List of requied argument information.
 * @param last Last argument information.
 */
class ArgumentInfoList(
    val requied: List<RequiedArgumentInfo>,
    val last: LastArgument
) {
    /**
     * Parses specified arguments to ArgumentData.
     */
    fun parse(args: List<String>): ParseResult {
        val validateResult = validateArguments(args)
        if (validateResult != null) {
            return validateResult
        }

        val arguments = HashMap<String, String>()
        for ((index, argumentInfo) in requied.withIndex()) {
            arguments[argumentInfo.name] = args[index]
        }

        when (last) {
            is OptionalArguments -> {
                parseOptionalArguments(last, arguments, args)
            }
            is LongArgumentInfo -> {
                parseLongArgument(last, arguments, args)
            }
        }

        return ParseResult.Success(arguments)
    }

    private fun parseOptionalArguments(
        optionalArguments: OptionalArguments,
        parseTo: MutableMap<String, String>,
        args: List<String>
    ) {
        for ((index, argumentInfo) in optionalArguments.withIndex()) {
            val argument = args.getOrElse(requied.size + index) {
                argumentInfo.defaultValue
            } ?: continue

            parseTo[argumentInfo.name] = argument
        }
    }

    private fun parseLongArgument(
        argumentInfo: LongArgumentInfo,
        parseTo: MutableMap<String, String>,
        args: List<String>
    ) {
        if (!argumentInfo.requied && requied.lastIndex + 1 > args.lastIndex) {
            return
        }
        val builder = StringBuilder()
        for (i in (requied.lastIndex + 1)..args.lastIndex) {
            if (builder.isNotEmpty()) {
                builder.append(' ')
            }
            builder.append(args[i])
        }
        parseTo[argumentInfo.name] = builder.toString()
    }

    private fun validateArguments(args: List<String>): ParseResult.Failed? {
        if (requied.size > args.size) {
            return ParseResult.Failed(ErrorType.REQUIED_ARGUMENT_NOT_ENOUGH)
        }
        when (last) {
            is LastArgument.NotAllow -> {
                if (requied.size < args.size) {
                    return ParseResult.Failed(ErrorType.TOO_MANY_ARGUMENTS)
                }
            }
            is LongArgumentInfo -> {
                if (last.requied && requied.lastIndex + 1 > args.lastIndex) {
                    return ParseResult.Failed(ErrorType.REQUIED_ARGUMENT_NOT_ENOUGH)
                }
            }
        }

        return null
    }
}

sealed class ParseResult {

    class Failed(val errorType: ErrorType) : ParseResult()

    class Success(val arguments: Map<String, String>) : ParseResult()
}

enum class ErrorType {
    REQUIED_ARGUMENT_NOT_ENOUGH,
    TOO_MANY_ARGUMENTS
}

sealed class LastArgument {
    /**
     * Means that not requied arguments are not allowed.
     */
    object NotAllow : LastArgument()
}

/**
 * List of optional argument information.
 *
 * @constructor
 *
 * @param arguments List of optional argument information.
 */
class OptionalArguments(
    val arguments: List<OptionalArgumentInfo>
) : LastArgument(), List<OptionalArgumentInfo> by arguments

/**
 * Information of optional argument.
 *
 * @constructor
 *
 * @param name Name of this argument.
 * @param defaultValue Default value of this argument.
 */
class OptionalArgumentInfo(
    val name: String,
    val defaultValue: String?
)

/**
 * Information of argument.
 *
 * @constructor
 *
 * @param name Name of argument.
 */
class RequiedArgumentInfo(val name: String)

/**
 * Infomration of long(space included) argument.
 *
 * @constructor
 *
 * @param name Name of this argument.
 * @param requied Requied or not requied.
 */
class LongArgumentInfo(
    val name: String,
    val requied: Boolean
) : LastArgument()
