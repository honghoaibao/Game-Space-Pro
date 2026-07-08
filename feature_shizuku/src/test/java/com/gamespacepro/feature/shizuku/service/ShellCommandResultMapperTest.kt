package com.gamespacepro.feature.shizuku.service

import com.gamespacepro.domain.shell.ShellCommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandResultMapperTest {

    @Test
    fun `a successful parcel with a real exit code maps to Success`() {
        val parcel = ShellCommandResultParcel(
            isSuccess = true,
            exitCode = 0,
            stdout = "out",
            stderr = "",
            executionTimeMillis = 42,
            errorMessage = null,
        )

        val result = parcel.toDomainResult()

        assertTrue(result is ShellCommandResult.Success)
        result as ShellCommandResult.Success
        assertEquals(0, result.exitCode)
        assertEquals("out", result.stdout)
        assertEquals(42, result.executionTimeMillis)
    }

    @Test
    fun `isSuccess true but a null exit code still maps to Failure`() {
        // Shouldn't happen in practice, but the mapper must not crash or
        // silently claim success without a real exit code.
        val parcel = ShellCommandResultParcel(
            isSuccess = true,
            exitCode = null,
            stdout = "",
            stderr = "",
            executionTimeMillis = 0,
            errorMessage = null,
        )

        assertTrue(parcel.toDomainResult() is ShellCommandResult.Failure)
    }

    @Test
    fun `isSuccess false maps to Failure and carries the error message as the cause`() {
        val parcel = ShellCommandResultParcel(
            isSuccess = false,
            exitCode = null,
            stdout = "",
            stderr = "",
            executionTimeMillis = 5,
            errorMessage = "process failed to start",
        )

        val result = parcel.toDomainResult()

        assertTrue(result is ShellCommandResult.Failure)
        result as ShellCommandResult.Failure
        assertEquals("process failed to start", result.cause?.message)
    }
}
