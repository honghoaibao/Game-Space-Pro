package com.gamespace.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Mục "5. Thermal Protection" — theo dõi nhiệt độ qua
 * [PowerManager.getCurrentThermalStatus] (Android 10+/API 29, ADR-003).
 * Trên API < 29, không có API nhiệt CPU công khai — [observeStatus] chỉ phát ra
 * [PowerManager.THERMAL_STATUS_NONE] một lần và không cập nhật thêm.
 */
@Singleton
class ThermalMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null

    fun currentStatus(): Int =
        if (isSupported) powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        else PowerManager.THERMAL_STATUS_NONE

    /** Phát trạng thái nhiệt hiện tại ngay khi subscribe, sau đó mỗi khi hệ thống báo thay đổi. */
    fun observeStatus(): Flow<Int> = callbackFlow {
        trySend(currentStatus())
        if (!isSupported) {
            awaitClose { }
            return@callbackFlow
        }
        val listener = PowerManager.OnThermalStatusChangedListener { status -> trySend(status) }
        powerManager?.addThermalStatusListener(listener)
        awaitClose { powerManager?.removeThermalStatusListener(listener) }
    }

    fun label(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Bình thường"
        PowerManager.THERMAL_STATUS_LIGHT -> "Hơi ấm"
        PowerManager.THERMAL_STATUS_MODERATE -> "Nóng vừa"
        PowerManager.THERMAL_STATUS_SEVERE -> "Nóng nghiêm trọng"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Rất nóng — nguy cơ giảm hiệu năng"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Khẩn cấp"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Sắp tắt máy vì quá nhiệt"
        else -> "Không xác định"
    }

    /** Ngưỡng để [ThermalGuard] tự hạ Profile (mục "Hạ Profile khi máy nóng"). */
    fun shouldDowngradeProfile(status: Int): Boolean = status >= PowerManager.THERMAL_STATUS_MODERATE

    /** Ngưỡng để coi là "đã mát" và khôi phục Profile trước đó. */
    fun hasCooledDown(status: Int): Boolean = status <= PowerManager.THERMAL_STATUS_LIGHT
}
