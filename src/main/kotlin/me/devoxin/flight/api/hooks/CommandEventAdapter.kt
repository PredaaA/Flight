package me.devoxin.flight.api.hooks

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.exceptions.BadArgument
import net.dv8tion.jda.api.Permission

interface CommandEventAdapter {

    /**
     * Invoked when an invalid argument is passed.
     */
    fun onBadArgument(ctx: Context, command: CommandFunction, error: BadArgument)

    /**
     * Invoked when the parser encounters an internal error.
     */
    fun onParseError(ctx: Context, command: CommandFunction, error: Throwable)

    /**
     * Invoked before a command is executed. Useful for logging command usage etc.
     *
     * @return True, if the command should still be executed
     */
    fun onCommandPreInvoke(ctx: Context, command: CommandFunction): Boolean

    /**
     * Invoked after a command has executed, regardless of whether the command execution encountered an error
     *
     * @param ctx
     *        The command context.
     * @param command
     *        The command that finished processing.
     * @param failed
     *        Whether the command encountered an error or not. You can use `onCommandError` to retrieve the error.
     */
    fun onCommandPostInvoke(ctx: Context, command: CommandFunction, failed: Boolean)

    /**
     * Invoked when a command encounters an error during execution.
     */
    fun onCommandError(ctx: Context, command: CommandFunction, error: Throwable)

    /**
     * Invoked when a command is executed while on cool-down.
     *
     * @param ctx
     *        The command context.
     * @param command
     *        The command that encountered the cool-down.
     * @param cooldown
     *        The remaining time of the cool-down, in milliseconds.
     */
    fun onCommandCooldown(ctx: Context, command: CommandFunction, cooldown: Long)

    /**
     * Invoked when a user lacks permissions to execute a command
     */
    fun onUserMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>)

    /**
     * Invoked when the bot lacks permissions to execute a command
     */
    fun onBotMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>)

}
