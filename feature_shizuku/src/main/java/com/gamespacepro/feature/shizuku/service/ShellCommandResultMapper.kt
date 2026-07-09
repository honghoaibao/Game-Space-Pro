package com.gamespacepro.feature.shizuku.service

import com.gamespacepro.domain.shell.ShellCommandResult

internal fun ShellCommandResultParcel.toDomainResult(): ShellCommandResult =
    if (isSuccess && exitCode != null) {
        ShellCommandResult.Success(
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            executionTimeMillis = executionTimeMillis,
        )
    } else {
        ShellCommandResult.Failure(
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            executionTimeMillis = executionTimeMillis,
            cause = errorMessage?.let(::RuntimeException),
        )
    }
