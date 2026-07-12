package com.gamespace.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.gamespace.hardware.HardwareDetector
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.optimizer.OptimizerEngine
import com.gamespace.shizuku.CapabilityDetector
import com.gamespace.shizuku.ShellExecutor
import com.gamespace.shizuku.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Diễn giải [ProfileConfig] thành hành động cụ thể (mục "1. Performance Profiles").
 *
 * Nguyên tắc (ADR-002/ADR-003): luôn thử đường "an toàn" (API công khai / Intent xác nhận)
 * trước; chỉ dùng Shizuku cho phần không có API công khai (đổi Game Mode của app khác,
 * ép refresh rate/độ phân giải toàn hệ thống), và luôn kiểm tra Capability trước khi gọi.
 *
 * ADR-006: "Giảm độ phân giải" (`wm size`) đổi độ phân giải TOÀN HỆ THỐNG (ảnh hưởng cả UI
 * của GAME SPACE, không riêng game), rủi ro cao nhất nếu app bị crash giữa chừng khi đang ở
 * độ phân giải thấp — [com.gamespace.recovery.RecoveryManager] (Phiên 4) chịu trách nhiệm
 * phục hồi `wm size reset` nếu phiên trước đó không tắt sạch (ADR-007).
 */
@Singleton
class ProfileEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDetector: CapabilityDetector,
    private val shellExecutor: ShellExecutor,
    private val optimizerEngine: OptimizerEngine,
    private val hardwareDetector: HardwareDetector,
    private val logManager: LogManager,
) {

    /**
     * @param targetPackage package của game đang/sắp chơi. Null khi chỉ áp Profile
     *   chung (không gắn với game cụ thể), lúc đó các hành động per-package sẽ bị bỏ qua.
     */
    suspend fun apply(type: ProfileType, targetPackage: String? = null): ProfileApplyResult {
        val config = ProfileConfig.forType(type)
        val applied = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val pendingIntents = mutableListOf<Pair<String, Intent>>()

        logManager.log(LogCategory.PROFILE, "Áp dụng ${type.name} cho ${targetPackage ?: "(toàn hệ thống)"}")

        applyRefreshRate(config, applied, skipped)
        applyResolution(config, applied, skipped)
        applyGameMode(config, targetPackage, applied, skipped)
        applyBatterySaverHint(config, pendingIntents)
        applyBatteryOptimizationWhitelist(config, targetPackage, pendingIntents)

        if (config.cleanBackgroundBeforeLaunch) {
            val optimizeResult = optimizerEngine.deepRamClean()
            if (optimizeResult.skippedActions.isNotEmpty()) {
                // Có thể bị chặn bởi cooldown chống dọn RAM liên tục (xem OptimizerEngine.deepRamClean) —
                // hiện đúng lý do thay vì báo nhầm "Dọn 0 tiến trình nền" khiến người dùng tưởng lỗi.
                skipped += optimizeResult.skippedActions
            } else {
                applied += "Dọn ${optimizeResult.backgroundAppsKilled} tiến trình nền " +
                    "(~${optimizeResult.ramFreedMb.toInt()}MB RAM giải phóng)"
            }
        }

        return ProfileApplyResult(type, applied, skipped, pendingIntents)
    }

    private suspend fun applyRefreshRate(
        config: ProfileConfig,
        applied: MutableList<String>,
        skipped: MutableList<String>,
    ) {
        if (!config.limitRefreshRate && !config.requestHighestRefreshRate) return

        if (!capabilityDetector.canOverrideSystemRefreshRate()) {
            skipped += "Đổi tần số quét toàn hệ thống (cần Shizuku — chưa cấp quyền)"
            return
        }

        val result = if (config.limitRefreshRate && config.targetRefreshRateHz != null) {
            shellExecutor.execute("settings put system peak_refresh_rate ${config.targetRefreshRateHz}.0")
        } else {
            // Performance Mode: bỏ giới hạn, để hệ thống tự chọn cao nhất hỗ trợ.
            shellExecutor.execute("settings delete system peak_refresh_rate")
        }

        when (result) {
            is ShellResult.Success -> applied += "Đặt tần số quét hệ thống (peak_refresh_rate)"
            else -> skipped += "Đổi tần số quét hệ thống thất bại: $result"
        }
    }

    private suspend fun applyGameMode(
        config: ProfileConfig,
        targetPackage: String?,
        applied: MutableList<String>,
        skipped: MutableList<String>,
    ) {
        if (!config.requestPerformanceGameMode && !config.requestBatteryGameMode) return
        if (targetPackage == null) {
            skipped += "Android Game Mode (chưa có package game mục tiêu)"
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            skipped += "Android Game Mode (yêu cầu Android 12+)"
            return
        }
        if (!capabilityDetector.canRunShellCommands()) {
            skipped += "Android Game Mode cho $targetPackage (cần Shizuku — chưa cấp quyền)"
            return
        }

        // Mode theo AOSP `cmd game`: 1=UNSUPPORTED/STANDARD, 2=PERFORMANCE, 3=BATTERY.
        val mode = if (config.requestPerformanceGameMode) 2 else 3
        val result = shellExecutor.execute("cmd game set --mode $mode $targetPackage")
        when (result) {
            is ShellResult.Success -> applied += "Game Mode ${if (mode == 2) "Performance" else "Battery"} cho $targetPackage"
            else -> skipped += "Đặt Game Mode cho $targetPackage thất bại: $result"
        }
    }

    private suspend fun applyResolution(
        config: ProfileConfig,
        applied: MutableList<String>,
        skipped: MutableList<String>,
    ) {
        if (!capabilityDetector.canOverrideResolution()) {
            if (config.downscaleResolution) skipped += "Giảm độ phân giải (cần Shizuku — chưa cấp quyền)"
            return
        }

        if (config.downscaleResolution) {
            val info = hardwareDetector.detect()
            if (info.screenWidthPx <= 0 || info.screenHeightPx <= 0) {
                skipped += "Giảm độ phân giải (không đọc được độ phân giải gốc)"
                return
            }
            val targetWidth = scaledEven(info.screenWidthPx)
            val targetHeight = scaledEven(info.screenHeightPx)
            val result = shellExecutor.execute("wm size ${targetWidth}x$targetHeight")
            when (result) {
                is ShellResult.Success ->
                    applied += "Giảm độ phân giải còn ${targetWidth}x$targetHeight (~${(RESOLUTION_SCALE * 100).toInt()}%)"
                else -> skipped += "Giảm độ phân giải thất bại: $result"
            }
        } else {
            // Đảm bảo không còn kẹt ở độ phân giải đã giảm từ Low Mode trước đó (idempotent).
            val result = shellExecutor.execute("wm size reset")
            if (result is ShellResult.Success) applied += "Đảm bảo độ phân giải gốc (wm size reset)"
        }
    }

    private fun scaledEven(pixels: Int): Int {
        val scaled = (pixels * RESOLUTION_SCALE).toInt()
        return scaled - (scaled % 2) // làm chẵn, tránh lỗi lẻ pixel trên vài driver GPU
    }

    companion object {
        private const val RESOLUTION_SCALE = 0.8
    }

    private fun applyBatterySaverHint(config: ProfileConfig, pendingIntents: MutableList<Pair<String, Intent>>) {
        if (!config.enableBatterySaverHint) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isPowerSaveMode == true) return // đã bật sẵn

        pendingIntents += "Bật chế độ tiết kiệm pin" to Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    }

    private fun applyBatteryOptimizationWhitelist(
        config: ProfileConfig,
        targetPackage: String?,
        pendingIntents: MutableList<Pair<String, Intent>>,
    ) {
        if (!config.suggestBatteryOptimizationWhitelist || targetPackage == null) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isIgnoringBatteryOptimizations(targetPackage) == true) return

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$targetPackage")
        }
        pendingIntents += "Thêm $targetPackage vào danh sách miễn tối ưu pin" to intent
    }
}
