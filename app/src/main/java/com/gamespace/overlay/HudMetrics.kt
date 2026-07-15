package com.gamespace.overlay

import com.gamespace.profile.ProfileType

data class HudMetrics(
    val fps: Int = 0,
    val ramAvailableMb: Int = 0,
    val ramTotalMb: Int = 0,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val thermalLabel: String = "Bình thường",
    /** Giá trị thô từ `PowerManager.THERMAL_STATUS_*` — dùng để tô màu theo mức độ, tách khỏi
     * [thermalLabel] (chuỗi hiển thị) để không phải so khớp chuỗi tiếng Việt trong UI. */
    val thermalStatus: Int = 0,
    /** null nếu không đọc được (cần Shizuku để đọc /proc/stat — xem [HudMetricsCollector]). */
    val cpuUsagePercent: Int? = null,
    val activeProfile: ProfileType = ProfileType.BALANCED,
) {
    val ramUsedPercent: Int
        get() = if (ramTotalMb == 0) 0 else (((ramTotalMb - ramAvailableMb) * 100) / ramTotalMb)
}
