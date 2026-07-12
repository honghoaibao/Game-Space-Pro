package com.gamespace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.gamespace.recovery.RecoveryManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application root. Khởi tạo Notification Channel dùng chung, chạy [RecoveryManager]
 * (mục "12. Recovery") ở cold start, và theo dõi vòng đời CẢ TIẾN TRÌNH (không phải
 * từng Activity) qua [ProcessLifecycleOwner] để đánh dấu "tắt sạch" hay "chưa sạch"
 * (dirty-bit pattern — xem KDoc trong `RecoveryManager`).
 */
@HiltAndroidApp
class GameSpaceApp : Application(), DefaultLifecycleObserver {

    @Inject lateinit var recoveryManager: RecoveryManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        applicationScope.launch {
            recoveryManager.checkAndRecoverIfNeeded()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Cả tiến trình lên foreground — bắt đầu coi phiên là "chưa tắt sạch" cho tới khi [onStop]. */
    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch { recoveryManager.markSessionActive() }
    }

    /** Cả tiến trình về nền bình thường — đây MỚI là "tắt sạch", không phải crash/bị kill. */
    override fun onStop(owner: LifecycleOwner) {
        applicationScope.launch { recoveryManager.markCleanShutdown() }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_OVERLAY,
                "Overlay HUD",
                NotificationManager.IMPORTANCE_MIN,
            ),
            NotificationChannel(
                CHANNEL_AUTOMATION,
                "Tự động hóa Game",
                NotificationManager.IMPORTANCE_MIN,
            ),
            NotificationChannel(
                CHANNEL_THERMAL,
                "Cảnh báo nhiệt độ",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        channels.forEach(manager::createNotificationChannel)
    }

    companion object {
        const val CHANNEL_OVERLAY = "channel_overlay_hud"
        const val CHANNEL_AUTOMATION = "channel_automation"
        const val CHANNEL_THERMAL = "channel_thermal"
    }
}
