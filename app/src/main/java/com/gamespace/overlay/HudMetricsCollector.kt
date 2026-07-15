package com.gamespace.overlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.Choreographer
import com.gamespace.hardware.CpuUsageReader
import com.gamespace.profile.ProfileRepository
import com.gamespace.thermal.ThermalMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Nguồn số liệu cho Floating HUD (mục "3. Game Space Overlay") — dùng chung cho cả
 * Overlay HUD (Phiên 3) và màn hình Performance Center (Phiên 6). Có thể có NHIỀU
 * collector cùng lúc (vd. Overlay HUD đang chạy trong lúc người dùng mở thêm màn
 * Performance Center), nên mỗi lần gọi [metricsFlow] tự tạo bộ đếm frame RIÊNG
 * (không dùng field chung cấp Singleton) để tránh 2 collector giẫm lên số đếm của nhau.
 *
 * FPS đo bằng [Choreographer], đếm số frame thực tế được vẽ trong 1 giây — chỉ phản
 * ánh frame của chính tiến trình GAME SPACE, không phải FPS thật của game đang chơi,
 * vì Android không cho app thứ ba đọc frame counter của app khác qua API công khai
 * (ghi rõ giới hạn này trong UI, xem `ARCHITECTURE.md` nếu cần bổ sung ADR).
 *
 * CPU% đọc qua [CpuUsageReader] (`/proc/stat`, fallback Shizuku) — trả `null` ở lần đo
 * đầu tiên (chưa có 2 mẫu để tính delta) hoặc khi thiết bị chặn hẳn cả 2 cách đọc; UI xử
 * lý `null` bằng cách ẩn dòng CPU thay vì hiện số sai.
 */
@Singleton
class HudMetricsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val thermalMonitor: ThermalMonitor,
    private val profileRepository: ProfileRepository,
    private val cpuUsageReader: CpuUsageReader,
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    fun metricsFlow(intervalMillis: Long = 1_000): Flow<HudMetrics> = flow {
        val frameCounter = AtomicInteger(0)
        val frameCallback = registerFrameCounter(frameCounter)
        try {
            while (true) {
                val fps = frameCounter.getAndSet(0).coerceIn(0, 240)
                emit(buildMetrics(fps))
                delay(intervalMillis)
            }
        } finally {
            unregisterFrameCounter(frameCallback)
        }
    }

    private suspend fun buildMetrics(fps: Int): HudMetrics {
        val memInfo = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val thermalStatus = thermalMonitor.currentStatus()
        return HudMetrics(
            fps = fps,
            ramAvailableMb = (memInfo.availMem / 1_048_576).toInt(),
            ramTotalMb = (memInfo.totalMem / 1_048_576).toInt(),
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            thermalLabel = thermalMonitor.label(thermalStatus),
            thermalStatus = thermalStatus,
            cpuUsagePercent = cpuUsageReader.readUsagePercent(),
            activeProfile = profileRepository.activeProfile.first(),
        )
    }

    private suspend fun registerFrameCounter(counter: AtomicInteger): Choreographer.FrameCallback =
        withContext(Dispatchers.Main) {
            lateinit var callback: Choreographer.FrameCallback
            callback = Choreographer.FrameCallback {
                counter.incrementAndGet()
                Choreographer.getInstance().postFrameCallback(callback)
            }
            Choreographer.getInstance().postFrameCallback(callback)
            callback
        }

    private suspend fun unregisterFrameCounter(callback: Choreographer.FrameCallback) {
        withContext(Dispatchers.Main) {
            Choreographer.getInstance().removeFrameCallback(callback)
        }
    }
}
