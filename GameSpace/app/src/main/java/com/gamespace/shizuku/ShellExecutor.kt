package com.gamespace.shizuku

import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

sealed class ShellResult {
    data class Success(val exitCode: Int, val output: String) : ShellResult()
    data class Failure(val reason: String) : ShellResult()
    /** Trả về khi chưa có quyền Shizuku — UI nên hiển thị hướng dẫn thay vì báo lỗi chung chung. */
    data object CapabilityUnavailable : ShellResult()
}

/**
 * Executor duy nhất cho mọi lệnh shell cấp cao (fstrim, pm clear, force-stop hàng loạt,
 * settings put system peak_refresh_rate...). Luôn kiểm tra [CapabilityDetector] trước,
 * luôn ghi log qua [LogManager] (mục "11. Logging").
 *
 * Lưu ý kiến trúc: bản này dùng `Shizuku.newProcess` (có ở shizuku-api <= 13.x qua reflection
 * nội bộ của thư viện). Nếu phiên bản Shizuku API sau này gỡ bỏ API này, cần chuyển sang
 * mô hình Shizuku UserService (AIDL) — ghi việc này vào TASK_BACKLOG.md khi xảy ra.
 */
@Singleton
class ShellExecutor @Inject constructor(
    private val capabilityDetector: CapabilityDetector,
    private val logManager: LogManager,
) {

    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!capabilityDetector.canRunShellCommands()) {
            logManager.log(LogCategory.SHIZUKU, "Từ chối thực thi (chưa cấp quyền): $command")
            return@withContext ShellResult.CapabilityUnavailable
        }

        logManager.log(LogCategory.SHELL_COMMAND, "Thực thi: $command")
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val exitCode = process.waitFor()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val errorOutput = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val combined = listOf(output, errorOutput).filter { it.isNotBlank() }.joinToString("\n")

            logManager.log(LogCategory.SHELL_COMMAND, "Kết quả ($exitCode): ${combined.take(500)}")
            ShellResult.Success(exitCode, combined)
        } catch (t: Throwable) {
            logManager.log(LogCategory.ERROR, "Lỗi thực thi '$command': ${t.message}")
            ShellResult.Failure(t.message ?: "Unknown error")
        }
    }

    /** fstrim toàn hệ thống — dùng cho Optimizer Engine "Storage Trim" (Phiên 2). */
    suspend fun fstrim(): ShellResult {
        if (!capabilityDetector.canRunFstrim()) return ShellResult.CapabilityUnavailable
        return execute("sm fstrim")
    }

    /** Force-stop một package — dùng cho Optimizer Engine (Phiên 2). */
    suspend fun forceStopPackage(packageName: String): ShellResult {
        if (!capabilityDetector.canForceStopOtherApps()) return ShellResult.CapabilityUnavailable
        return execute("am force-stop $packageName")
    }
}
