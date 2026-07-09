package com.gamespacepro.domain.shell

/**
 * Runs a single shell command to completion.
 *
 * Implementations are expected to detect and report failure (backend
 * unavailable, permission missing, process couldn't start) as a
 * [ShellCommandResult.Failure] rather than throwing, so callers don't need
 * a try/catch around every call — only genuinely unexpected errors should
 * escape as exceptions.
 *
 * No implementation exists yet. The Shizuku-backed one requires an AIDL
 * `UserService` (Shizuku deprecated the simpler `newProcess` API in recent
 * releases — see `feature_shizuku/README.md`), which is enough moving,
 * hard-to-verify-without-a-device machinery that it's scoped as its own
 * follow-up rather than bundled into this pass.
 */
interface ShellExecutor {
    suspend fun execute(command: List<String>): ShellCommandResult
}
