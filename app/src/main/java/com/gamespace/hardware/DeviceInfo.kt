package com.gamespace.hardware

/**
 * Kết quả tổng hợp của [HardwareDetector]. Dùng để hiển thị ở Dashboard/Diagnostics
 * và để [com.gamespace.profile.ProfileEngine] gợi ý Profile phù hợp.
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val supportedAbis: List<String>,
    val cpuCoreCount: Int,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val supportedRefreshRates: List<Float>,
    val currentRefreshRate: Float,
    val supportsGameMode: Boolean, // GameManager API, Android 12+ (API 31)
    val supportsThermalApi: Boolean, // PowerManager thermal status, Android 10+ (API 29)
    val shizukuInstalled: Boolean,
    val shizukuGranted: Boolean,
    val isLowRamDevice: Boolean,
) {
    val totalRamGb: Double get() = totalRamBytes / 1_073_741_824.0
    val availableRamGb: Double get() = availableRamBytes / 1_073_741_824.0

    /** Gợi ý Profile mặc định dựa trên phần cứng phát hiện được. */
    fun suggestedProfile(): com.gamespace.profile.ProfileType = when {
        isLowRamDevice || totalRamGb < 3.0 -> com.gamespace.profile.ProfileType.LOW
        totalRamGb >= 6.0 && supportedRefreshRates.any { it >= 90f } ->
            com.gamespace.profile.ProfileType.PERFORMANCE
        else -> com.gamespace.profile.ProfileType.BALANCED
    }
}
