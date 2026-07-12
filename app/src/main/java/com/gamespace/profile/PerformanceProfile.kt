package com.gamespace.profile

enum class ProfileType(val displayName: String, val emoji: String) {
    LOW("Low Mode", "🟢"),
    BALANCED("Balanced Mode", "🔵"),
    PERFORMANCE("Performance Mode", "🔴"),
}

/**
 * Cấu hình hành vi cho từng Profile. Các cờ boolean tương ứng 1-1 với các mục
 * trong đặc tả "1. Performance Profiles" — mỗi cờ được [ProfileEngine] diễn giải
 * thành hành động cụ thể, luôn kiểm tra Capability trước khi thực thi.
 */
data class ProfileConfig(
    val type: ProfileType,
    /** Giới hạn tần số quét (Low Mode) — true = ép về [targetRefreshRateHz]. */
    val limitRefreshRate: Boolean,
    val targetRefreshRateHz: Int?,
    /** Yêu cầu tần số quét cao nhất thiết bị hỗ trợ (Performance Mode). */
    val requestHighestRefreshRate: Boolean,
    /** Gợi ý người dùng bật chế độ tiết kiệm pin hệ thống (Low Mode). */
    val enableBatterySaverHint: Boolean,
    /** Kích hoạt Android Game Mode Performance qua GameManager (API 31+, Performance Mode). */
    val requestPerformanceGameMode: Boolean,
    /** Kích hoạt Android Game Mode Battery qua GameManager (API 31+, Low Mode). */
    val requestBatteryGameMode: Boolean,
    /** Gợi ý thêm game vào danh sách miễn tối ưu pin (Performance Mode). */
    val suggestBatteryOptimizationWhitelist: Boolean,
    /** Dọn tiến trình nền trước khi vào game. */
    val cleanBackgroundBeforeLaunch: Boolean,
    /** Giảm độ phân giải hệ thống (~80% gốc) để giảm tải GPU — chỉ Low Mode, cần Shizuku. */
    val downscaleResolution: Boolean,
) {
    companion object {
        fun forType(type: ProfileType): ProfileConfig = when (type) {
            ProfileType.LOW -> ProfileConfig(
                type = ProfileType.LOW,
                limitRefreshRate = true,
                targetRefreshRateHz = 60,
                requestHighestRefreshRate = false,
                enableBatterySaverHint = true,
                requestPerformanceGameMode = false,
                requestBatteryGameMode = true,
                suggestBatteryOptimizationWhitelist = false,
                cleanBackgroundBeforeLaunch = true,
                downscaleResolution = true,
            )
            ProfileType.BALANCED -> ProfileConfig(
                type = ProfileType.BALANCED,
                limitRefreshRate = false,
                targetRefreshRateHz = null,
                requestHighestRefreshRate = false,
                enableBatterySaverHint = false,
                requestPerformanceGameMode = false,
                requestBatteryGameMode = false,
                suggestBatteryOptimizationWhitelist = false,
                cleanBackgroundBeforeLaunch = true,
                downscaleResolution = false,
            )
            ProfileType.PERFORMANCE -> ProfileConfig(
                type = ProfileType.PERFORMANCE,
                limitRefreshRate = false,
                targetRefreshRateHz = null,
                requestHighestRefreshRate = true,
                enableBatterySaverHint = false,
                requestPerformanceGameMode = true,
                requestBatteryGameMode = false,
                suggestBatteryOptimizationWhitelist = true,
                cleanBackgroundBeforeLaunch = true,
                downscaleResolution = false,
            )
        }
    }
}
