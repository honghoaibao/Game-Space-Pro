package com.gamespace.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Accessibility Engine (mục "8."). HOÀN TOÀN TÙY CHỌN — người dùng tự bật thủ công
 * trong Settings > Accessibility (Android không cho phép app tự bật service này).
 * Không tính năng lõi nào (Automation, Profile, Overlay) phụ thuộc vào service này
 * hoạt động — nó chỉ tăng độ chính xác/tốc độ phát hiện qua [AccessibilityStateBus].
 *
 * Việc dùng: phát hiện app foreground nhanh hơn UsageStatsManager (vốn có độ trễ polling),
 * và phát hiện dialog xuất hiện (UsageStatsManager không làm được) — có thể dùng sau này
 * để Overlay tự ẩn khi có dialog hệ thống che khuất, tránh chặn thao tác của người dùng.
 */
@AndroidEntryPoint
class GameSpaceAccessibilityService : AccessibilityService() {

    @Inject lateinit var stateBus: AccessibilityStateBus

    @Inject lateinit var logManager: LogManager

    private var serviceScope: CoroutineScope? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        stateBus.updateServiceConnected(true)
        fireAndForgetLog("Accessibility Engine đã kết nối")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowStateChanged(e)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> Unit // theo dõi nội dung — để dành cho phiên sau nếu cần
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        // Bỏ qua sự kiện từ chính GAME SPACE để không nhiễu dữ liệu foreground.
        if (packageName == this.packageName) return

        stateBus.updateForegroundPackage(packageName)

        val className = event.className?.toString().orEmpty()
        val looksLikeDialog = className.contains("Dialog", ignoreCase = true) ||
            className.contains("AlertDialog", ignoreCase = true)
        stateBus.updateDialogShowing(looksLikeDialog)
    }

    /** Log không phụ thuộc `serviceScope` (an toàn nếu gọi trước khi service kết nối xong). */
    private fun fireAndForgetLog(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logManager.log(LogCategory.SHELL_COMMAND, message)
        }
    }

    override fun onInterrupt() {
        // Không cần xử lý đặc biệt hiện tại.
    }

    override fun onDestroy() {
        stateBus.updateServiceConnected(false)
        stateBus.updateForegroundPackage(null)
        stateBus.updateDialogShowing(false)
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }
}
