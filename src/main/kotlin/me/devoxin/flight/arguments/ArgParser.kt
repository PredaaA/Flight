package me.devoxin.flight.arguments

import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.exceptions.BadArgument
import me.devoxin.flight.api.Context
import me.devoxin.flight.exceptions.ParserNotRegistered
import me.devoxin.flight.parsers.Parser
import org.slf4j.LoggerFactory
import java.util.Optional

class ArgParser(
        private val ctx: Context,
        commandArgs: List<String>,
        private val delimiter: Char
) {
    private var args = commandArgs.toMutableList()

    private fun getArgs(amount: Int): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        val taken = args.take(amount)
        args.drop(amount)

        return taken
    }

    private fun parseNextArgument(consumeRest: Boolean = false): String {
        if (delimiter != ' ') {
            val argument = args.firstOrNull() ?: ""
            args = args.subList(1, args.size)
            return argument.trim()
        }

        if (args.isEmpty()) {
            return ""
        } else {
            if (consumeRest) {
                return getArgs(args.size).joinToString(" ")
            }
        }

        val isQuoted = args[0].startsWith('"') // Quotes! TODO: accept other forms of quote chars

        if (!isQuoted || delimiter != ' ') { // Don't handle quote arguments if a custom delimiter was specified.
            return getArgs(1).joinToString(" ")
        }

        val iterator = args.joinToString(" ").iterator()
        val argument = StringBuilder()
        var quoting = false
        var escaping = false

        while (iterator.hasNext()) {
            val char = iterator.nextChar()

            if (escaping) {
                argument.append(char)
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else if (quoting && char == '"') { // TODO: accept other forms of quote chars
                quoting = false
            } else if (!quoting && char == '"') { // TODO: accept other forms of quote chars
                quoting = true
            } else if (!quoting && char == delimiter) { // char.isWhitespace
                // If we're not quoting and it's not whitespace we should throw
                // ex: !test  blah -- Extraneous whitespace. Currently we ignore this
                // (effectively "trimming" the argument) and just use the next part.
                if (argument.isEmpty()) {
                    continue
                } else {
                    break
                }
            } else {
                argument.append(char)
            }
        }

        val remainingArgs = StringBuilder()
        iterator.forEachRemaining { remainingArgs.append(it) }
        args = remainingArgs.toString().split(" +".toRegex()).toMutableList()

        return argument.toString().trim()
    }

    fun parse(arg: Argument): Any? {
        val argument = parseNextArgument(arg.greedy)
        val parser = parsers[arg.type]
                ?: throw ParserNotRegistered("No parsers registered for `${arg.type}`")

        val result: Optional<out Any?>
        result = if (argument.isEmpty()) {
            Optional.empty()
        } else {
            try {
                parser.parse(ctx, argument)
            } catch (e: Exception) {
                throw BadArgument(arg, argument, e)
            }
        }

        if (!result.isPresent && arg.required) {
            throw BadArgument(arg, argument)
        }

        return result.orElse(null)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArgParser::class.java)
        val parsers = hashMapOf<Class<*>, Parser<*>>()

        fun parseArguments(cmd: CommandWrapper, ctx: Context, args: List<String>): Array<Any?> {
            if (cmd.arguments.isEmpty()) {
                return emptyArray()
            }

            val delimiter = cmd.properties.argDelimiter
            val commandArgs = if (delimiter == ' ') {
                args
            } else {
                args.joinToString(" ").split(delimiter).toMutableList()
            }

            val parser = ArgParser(ctx, commandArgs, delimiter)
            return cmd.arguments.map(parser::parse).toTypedArray()
        }
    }
}
