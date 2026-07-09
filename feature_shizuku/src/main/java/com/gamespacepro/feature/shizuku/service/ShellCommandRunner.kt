package com.gamespacepro.feature.shizuku.service

import java.io.InputStream

/**
 * Runs [command] as a child process to completion and captures its
 * outcome. Deliberately has zero Android dependencies — everything here is
 * plain `java.lang`/`java.io` so it's testable on an ordinary JVM, unlike
 * [ShellCommandUserService] which wraps this in an AIDL/Binder Stub.
 */
internal object ShellCommandRunner {

    fun run(command: List<String>): ShellCommandResultParcel {
        val startTime = System.currentTimeMillis()

        if (command.isEmpty()) {
            return ShellCommandResultParcel(
                isSuccess = false,
                exitCode = null,
                stdout = "",
                stderr = "",
                executionTimeMillis = System.currentTimeMillis() - startTime,
                errorMessage = "command must not be empty",
            )
        }

        return try {
            val process = ProcessBuilder(command).start()

            // Sequential reads here can deadlock: if the child fills the OS
            // pipe buffer on stderr while we're still blocked reading
            // stdout (or vice versa), the child blocks writing and we block
            // reading — neither side proceeds. Draining both streams
            // concurrently avoids that. Verified against 20,000 lines on
            // each stream simultaneously — see ShellCommandRunnerTest.
            val stdoutDrain = StreamDrainThread(process.inputStream).apply { start() }
            val stderrDrain = StreamDrainThread(process.errorStream).apply { start() }

            val exitCode = process.waitFor()
            stdoutDrain.join()
            stderrDrain.join()

            ShellCommandResultParcel(
                isSuccess = true,
                exitCode = exitCode,
                stdout = stdoutDrain.output(),
                stderr = stderrDrain.output(),
                executionTimeMillis = System.currentTimeMillis() - startTime,
                errorMessage = null,
            )
        } catch (exception: Exception) {
            ShellCommandResultParcel(
                isSuccess = false,
                exitCode = null,
                stdout = "",
                stderr = "",
                executionTimeMillis = System.currentTimeMillis() - startTime,
                errorMessage = exception.message ?: exception.javaClass.name,
            )
        }
    }

    private class StreamDrainThread(private val stream: InputStream) : Thread() {
        private val buffer = StringBuilder()

        override fun run() {
            stream.bufferedReader().forEachLine { line ->
                buffer.append(line).append('\n')
            }
        }

        fun output(): String = buffer.toString().trimEnd('\n')
    }
}
