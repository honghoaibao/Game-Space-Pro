package com.gamespacepro.feature.shizuku.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Runs real subprocesses via `sh`, so these only run where a POSIX shell is
 * available (true for the Linux CI runner and any Linux/macOS dev
 * machine; skipped elsewhere via [assumeTrue] rather than failing).
 */
class ShellCommandRunnerTest {

    private val hasShell = File("/bin/sh").canExecute()

    @Test
    fun `captures stdout, stderr, and a zero exit code separately`() {
        assumeTrue(hasShell)

        val result = ShellCommandRunner.run(
            listOf("sh", "-c", "echo hello-stdout; echo hello-stderr 1>&2; exit 0"),
        )

        assertTrue(result.isSuccess)
        assertEquals(0, result.exitCode)
        assertEquals("hello-stdout", result.stdout)
        assertEquals("hello-stderr", result.stderr)
    }

    @Test
    fun `a completed process with a non-zero exit code is still isSuccess`() {
        assumeTrue(hasShell)

        val result = ShellCommandRunner.run(listOf("sh", "-c", "exit 7"))

        assertTrue(result.isSuccess)
        assertEquals(7, result.exitCode)
    }

    @Test
    fun `empty command is rejected without touching ProcessBuilder`() {
        val result = ShellCommandRunner.run(emptyList())

        assertFalse(result.isSuccess)
        assertNull(result.exitCode)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `a command that can't be started reports failure instead of throwing`() {
        val result = ShellCommandRunner.run(listOf("this-command-does-not-exist-xyz"))

        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
    }

    /**
     * The one this whole class exists to protect: sequential (not
     * concurrent) draining of stdout/stderr deadlocks once either pipe's
     * OS buffer fills, because the child blocks writing while we're
     * blocked reading the other stream. 20,000 lines on each stream is
     * comfortably past typical 64KB pipe buffers. A regression back to
     * sequential draining would hang this test until JUnit's timeout below
     * fires, rather than silently pass.
     */
    @Test(timeout = 15_000)
    fun `large concurrent stdout and stderr output does not deadlock`() {
        assumeTrue(hasShell)

        val lineCount = 20_000
        val script = "i=0; while [ \$i -lt $lineCount ]; do " +
            "echo \"stdout line \$i\"; echo \"stderr line \$i\" 1>&2; i=\$((i+1)); done"

        val result = ShellCommandRunner.run(listOf("sh", "-c", script))

        assertTrue(result.isSuccess)
        assertEquals(0, result.exitCode)
        assertEquals(lineCount, result.stdout.lines().size)
        assertEquals(lineCount, result.stderr.lines().size)
    }
}
