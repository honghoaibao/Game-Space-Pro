package com.gamespace.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import rikka.shizuku.Shizuku

/**
 * Đọc toàn bộ thông tin phần cứng + khả năng hệ thống mà GAME SPACE cần để
 * quyết định Profile và bật/tắt tính năng theo Capability Matrix (ADR-002, ARCHITECTURE.md).
 *
 * Chỉ dùng API công khai của Android — không truy cập file hệ thống ngoài phạm vi
 * cho phép, không cần root.
 */
@Singleton
class HardwareDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun detect(): DeviceInfo {
        val activityManager = context.getSystemService<ActivityManager>()
        val memInfo = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val (supportedRates, currentRate) = readRefreshRates()
        val (screenW, screenH) = readScreenResolution()

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            apiLevel = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            totalRamBytes = memInfo.totalMem,
            availableRamBytes = memInfo.availMem,
            screenWidthPx = screenW,
            screenHeightPx = screenH,
            supportedRefreshRates = supportedRates,
            currentRefreshRate = currentRate,
            supportsGameMode = supportsGameMode(),
            supportsThermalApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            shizukuInstalled = isShizukuInstalled(),
            shizukuGranted = isShizukuPermissionGranted(),
            isLowRamDevice = activityManager?.isLowRamDevice ?: false,
        )
    }

    /**
     * CRASH FIX: `WindowManager.getDefaultDisplay()` gọi trên Application Context (không
     * phải Activity/window context) nội bộ vẫn ủy quyền qua `Context.getDisplay()`, nên vẫn
     * ném `UnsupportedOperationException: Tried to obtain display from a Context not
     * associated with one` trên app có targetSdk 30+ chạy trên máy Android 11+ (API 30+) —
     * bất kể có dùng API `getDefaultDisplay()` deprecated hay không (comment cũ ở đây ghi sai
     * là "an toàn cho mọi API level", đây chính là nguyên nhân crash ngay khi mở app vì
     * Dashboard gọi `detect()` ngay ở cold start). `DisplayManager.getDisplay(int)` KHÔNG có
     * ràng buộc UI-context này, dùng được an toàn từ Application Context ở mọi API level.
     */
    private fun readRefreshRates(): Pair<List<Float>, Float> {
        val displayManager = context.getSystemService<DisplayManager>() ?: return emptyList<Float>() to 0f
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return emptyList<Float>() to 0f

        val rates = display.supportedModes
            .map { it.refreshRate }
            .distinct()
            .sorted()
        return rates to display.refreshRate
    }

    private fun readScreenResolution(): Pair<Int, Int> {
        val windowManager = context.getSystemService<WindowManager>() ?: return 0 to 0
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = android.graphics.Point()
            @Suppress("DEPRECATION")
            display.getRealSize(point)
            point.x to point.y
        }
    }

    /** GameManager (android.app.GameManager) chỉ có từ Android 12 (API 31). */
    private fun supportsGameMode(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return context.getSystemService(android.app.GameManager::class.java) != null
    }

    /**
     * Kiểm tra Shizuku có được cài đặt/chạy hay không.
     * Dùng try/catch vì binder có thể chưa sẵn sàng — không được để crash toàn app
     * (ADR-002: Shizuku là nâng cao, luôn phải có fallback an toàn).
     */
    private fun isShizukuInstalled(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun isShizukuPermissionGranted(): Boolean = try {
        if (!Shizuku.pingBinder()) {
            false
        } else if (Shizuku.isPreV11()) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (_: Throwable) {
        false
    }
}
