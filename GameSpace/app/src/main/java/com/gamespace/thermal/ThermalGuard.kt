package com.gamespace.thermal

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.gamespace.GameSpaceApp
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.profile.ProfileEngine
import com.gamespace.profile.ProfileRepository
import com.gamespace.profile.ProfileType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Kết nối [ThermalMonitor] với [ProfileEngine]: tự hạ Profile khi máy nóng, tự khôi
 * phục khi mát, cảnh báo qua Notification (mục "5. Thermal Protection").
 *
 * Vòng đời: [run] là một vòng lặp `collect` vô hạn — gọi trong `coroutineScope` của
 * [com.gamespace.automation.GameDetectionService] để theo dõi song song với Automation
 * (ADR-005: Thermal Guard chạy kèm Automation Service, không phải service riêng, để
 * tránh thêm một Foreground Service nữa chỉ để theo dõi nhiệt độ).
 */
@Singleton
class ThermalGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val thermalMonitor: ThermalMonitor,
    private val profileRepository: ProfileRepository,
    private val profileEngine: ProfileEngine,
    private val logManager: LogManager,
) {
    /** Profile trước khi bị Thermal Guard ép hạ xuống LOW — null nếu chưa can thiệp. */
    private var preThermalProfile: ProfileType? = null

    suspend fun run() {
        thermalMonitor.observeStatus().collect { status -> handleStatus(status) }
    }

    private suspend fun handleStatus(status: Int) {
        val current = profileRepository.activeProfile.first()

        when {
            thermalMonitor.shouldDowngradeProfile(status) && current != ProfileType.LOW -> {
                if (preThermalProfile == null) preThermalProfile = current
                profileRepository.setActiveProfile(ProfileType.LOW)
                profileEngine.apply(ProfileType.LOW)
                logManager.log(LogCategory.THERMAL, "Máy nóng (${thermalMonitor.label(status)}) — tự hạ về Low Mode")
                notifyThermal(status)
            }
            thermalMonitor.hasCooledDown(status) && preThermalProfile != null -> {
                val restore = preThermalProfile ?: ProfileType.BALANCED
                preThermalProfile = null
                profileRepository.setActiveProfile(restore)
                profileEngine.apply(restore)
                logManager.log(LogCategory.THERMAL, "Máy đã mát — khôi phục ${restore.name}")
            }
        }
    }

    private fun notifyThermal(status: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = NotificationCompat.Builder(context, GameSpaceApp.CHANNEL_THERMAL)
            .setContentTitle("Thiết bị đang nóng")
            .setContentText("${thermalMonitor.label(status)} — đã tự chuyển sang Low Mode để bảo vệ máy")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 3001
    }
}
