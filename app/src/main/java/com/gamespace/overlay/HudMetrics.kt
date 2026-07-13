package com.gamespace.overlay

import com.gamespace.profile.ProfileType

data class HudMetrics(
    val fps: Int = 0,
    val ramAvailableMb: Int = 0,
    val ramTotalMb: Int = 0,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val thermalLabel: String = "Bình thường",
    /** null nếu không đọc được (cần Shizuku để đọc /proc/stat — xem [HudMetricsCollector]). */
    val cpuUsagePercent: Int? = null,
    val activeProfile: ProfileType = ProfileType.BALANCED,
) {
    val ramUsedPercent: Int
        get() = if (ramTotalMb == 0) 0 else (((ramTotalMb - ramAvailableMb) * 100) / ramTotalMb)
}
