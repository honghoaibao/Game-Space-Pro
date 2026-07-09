package com.gamespacepro.domain.shell

/**
 * Outcome of a single shell command, per the product spec's requirement
 * that every command report success/failure, exit code, stdout, stderr,
 * and execution time — regardless of which backend (Shizuku today; root
 * or something else potentially later) actually ran it.
 */
sealed class ShellCommandResult {
    abstract val stdout: String
    abstract val stderr: String
    abstract val executionTimeMillis: Long

    /** The process ran to completion. [exitCode] may still be non-zero. */
    data class Success(
        val exitCode: Int,
        override val stdout: String,
        override val stderr: String,
        override val executionTimeMillis: Long,
    ) : ShellCommandResult()

    /**
     * The command could not be run to completion at all — backend
     * unavailable, permission missing, process failed to start, etc.
     * [exitCode] is `null` when the process never produced one.
     */
    data class Failure(
        val exitCode: Int?,
        override val stdout: String,
        override val stderr: String,
        override val executionTimeMillis: Long,
        val cause: Throwable? = null,
    ) : ShellCommandResult()
}
