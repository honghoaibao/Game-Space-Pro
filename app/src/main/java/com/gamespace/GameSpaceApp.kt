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
import kotlinx.coroutines.CoroutineExceptionHandler
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

    private val applicationScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                // Bắt buộc phải có handler ở đây: applicationScope chạy `checkAndRecoverIfNeeded()`
                // (Recovery — vốn dĩ chỉ nên CHẨN ĐOÁN sự cố phiên trước, không được TỰ nó trở
                // thành nguyên nhân crash mới) và các lần cập nhật dirty-bit onStart/onStop suốt
                // vòng đời app. SupervisorJob KHÔNG tự nuốt exception — thiếu handler này, bất kỳ
                // lỗi nào trong DataStore/Room/Shizuku bên dưới (kể cả trên máy hiếm gặp/OEM lạ)
                // sẽ crash toàn app ngay khi mở, đúng vào bug report "vẫn crash khi mở app".
                android.util.Log.e("GameSpaceApp", "Lỗi không mong muốn trong applicationScope", throwable)
            },
    )

    override fun onCreate() {
        super<Application>.onCreate()
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
            // IMPORTANCE_LOW (không phải MIN): vẫn im lặng, không heads-up, không làm phiền khi
            // đang chơi game — nhưng KHÔNG bị ẩn khỏi status bar/shade như MIN. Nhiều OEM (Xiaomi,
            // Oppo, Vivo...) coi notification bị ẩn (MIN) là dấu hiệu Foreground Service "không còn
            // ai để ý" và dọn/tắt nền mạnh tay hơn — đây là nguyên nhân phổ biến khiến popup HUD tự
            // tắt/biến mất dù đã bật Foreground Service + quyền overlay đúng (báo cáo lỗi: "popup bị
            // tắt hay mất"). Kết hợp với setPriority + setCategory(CATEGORY_SERVICE) ở từng Service
            // (OverlayService, FloatingWebViewService, GameDetectionService) để hệ thống nhận diện
            // đây là service đang hoạt động thật, không phải nền im lặng có thể dọn bất cứ lúc nào.
            NotificationChannel(
                CHANNEL_OVERLAY,
                "Overlay HUD",
                NotificationManager.IMPORTANCE_LOW,
            ),
            NotificationChannel(
                CHANNEL_AUTOMATION,
                "Tự động hóa Game",
                NotificationManager.IMPORTANCE_LOW,
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
