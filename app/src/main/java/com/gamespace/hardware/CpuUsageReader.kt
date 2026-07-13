package com.gamespace.hardware

import com.gamespace.shizuku.CapabilityDetector
import com.gamespace.shizuku.ShellExecutor
import com.gamespace.shizuku.ShellResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Đọc % CPU toàn hệ thống bằng công thức chuẩn: lấy 2 mẫu dòng `cpu ` đầu tiên của
 * `/proc/stat` cách nhau 1 khoảng thời gian, rồi tính:
 *   cpu% = 100 * (1 - idleDelta / totalDelta)
 * (xem `man proc` — cột 4 là idle, cột 5 là iowait, cộng cả 2 vào "thời gian rảnh").
 *
 * Trước đây [com.gamespace.overlay.HudMetricsCollector] luôn trả `null` cho mục này
 * (xem ghi chú cũ trong TASK_BACKLOG.md Phiên 3/6) — đây là phần triển khai thật:
 *
 * 1. Thử đọc trực tiếp `/proc/stat` trước — dòng tổng "cpu " (không phải "cpu0", "cpu1"...)
 *    là số liệu tổng hợp toàn hệ thống, KHÔNG phải thông tin riêng của tiến trình khác,
 *    nên phần lớn thiết bị Android (kể cả không root/không Shizuku) vẫn cho app đọc được.
 * 2. Nếu bị SELinux/OEM chặn (một số ROM khóa `/proc/stat` với app thường), fallback qua
 *    Shizuku (`cat /proc/stat`) nếu người dùng đã cấp quyền.
 * 3. Nếu cả 2 đều không đọc được, trả `null` — [com.gamespace.overlay.HudContent] và
 *    Performance Center đã xử lý sẵn `null` bằng cách ẩn dòng CPU thay vì hiện số sai.
 *
 * Cần đúng 2 mẫu liên tiếp mới tính được %, nên lần gọi đầu tiên sau khi tạo/khởi động
 * lại luôn trả `null` (chưa có mẫu trước đó để so sánh) — đây là hành vi đúng, không phải lỗi.
 */
@Singleton
class CpuUsageReader @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val capabilityDetector: CapabilityDetector,
) {
    private val mutex = Mutex()
    private var lastTotal: Long = -1
    private var lastIdle: Long = -1

    suspend fun readUsagePercent(): Int? = mutex.withLock {
        val line = readCpuLine() ?: return@withLock null
        val values = line.trim()
            .removePrefix("cpu")
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.toLongOrNull() }
        // user nice system idle iowait irq softirq steal [guest guest_nice]
        if (values.size < 4) return@withLock null

        val idle = values[3] + values.getOrElse(4) { 0L }
        val total = values.sum()

        val result = if (lastTotal >= 0 && total > lastTotal) {
            val totalDelta = total - lastTotal
            val idleDelta = (idle - lastIdle).coerceAtLeast(0)
            (100 - (idleDelta * 100 / totalDelta)).toInt().coerceIn(0, 100)
        } else {
            null
        }
        lastTotal = total
        lastIdle = idle
        result
    }

    private suspend fun readCpuLine(): String? {
        readProcStatDirect()?.let { return it }
        return readProcStatViaShizuku()
    }

    private suspend fun readProcStatDirect(): String? = withContext(Dispatchers.IO) {
        runCatching {
            File("/proc/stat").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("cpu ") }
            }
        }.getOrNull()
    }

    private suspend fun readProcStatViaShizuku(): String? {
        if (!capabilityDetector.canRunShellCommands()) return null
        val result = shellExecutor.execute("cat /proc/stat")
        val output = (result as? ShellResult.Success)?.output ?: return null
        return output.lineSequence().firstOrNull { it.startsWith("cpu ") }
    }
}
