package com.gamespace.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gamespace.GameSpaceApp
import com.gamespace.R
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.permissions.PermissionChecker
import com.gamespace.ui.theme.GameSpaceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Popup Apps — mục "1. Popup Apps (Floating Apps)" trong đặc tả mở rộng.
 *
 * Chỉ vẽ nội dung WEB nổi do chính GAME SPACE kiểm soát (WebView riêng) — KHÔNG ép
 * các app khác (Chrome/TikTok/Zalo thật) chạy trong overlay, vì Android không cho
 * app thứ ba làm điều đó (xem ADR-001 trong `ARCHITECTURE.md`). Dùng để tra cứu
 * wiki/hướng dẫn/ChatGPT/Gemini bản web mà không cần rời khỏi game.
 */
@AndroidEntryPoint
class FloatingWebViewService : Service() {

    @Inject lateinit var permissionChecker: PermissionChecker

    @Inject lateinit var logManager: LogManager

    private var serviceScope: CoroutineScope? = null
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var lastExpandedWidthPx = 0
    private var lastExpandedHeightPx = 0
    private var webViewRef: WebView? = null

    private var browserState by mutableStateOf(FloatingBrowserState())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        val requestedUrl = intent?.getStringExtra(EXTRA_URL)
        if (composeView == null) {
            if (!permissionChecker.hasOverlayPermission()) {
                fireAndForgetLog("Floating Browser: thiếu quyền SYSTEM_ALERT_WINDOW — dừng service")
                showToast("Chưa cấp quyền hiển thị popup — cấp quyền \"Hiển thị trên ứng dụng khác\" ở Dashboard rồi thử lại.")
                stopSelf()
                return START_NOT_STICKY
            }
            setupOverlay(requestedUrl ?: FloatingBrowserState.DEFAULT_URL)
        } else if (requestedUrl != null) {
            navigate(requestedUrl)
            browserState = browserState.copy(isMinimized = false)
        }
        return START_STICKY
    }

    private fun setupOverlay(initialUrl: String) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        serviceScope = scope
        browserState = FloatingBrowserState(currentUrl = initialUrl, addressBarText = initialUrl)

        val owner = OverlayLifecycleOwner().apply {
            performRestore()
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = resources.displayMetrics.density
        lastExpandedWidthPx = (DEFAULT_WIDTH_DP * density).roundToInt()
        lastExpandedHeightPx = (DEFAULT_HEIGHT_DP * density).roundToInt()

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            lastExpandedWidthPx,
            lastExpandedHeightPx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                GameSpaceTheme(darkTheme = true) {
                    FloatingBrowserContent(
                        state = browserState,
                        onAddressChange = { text -> browserState = browserState.copy(addressBarText = text) },
                        onNavigate = ::navigate,
                        onGoBack = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                        onToggleMinimize = ::toggleMinimize,
                        onToggleLock = { browserState = browserState.copy(isLocked = !browserState.isLocked) },
                        onCycleAlpha = ::cycleAlpha,
                        onToggleTouchGuard = {
                            browserState = browserState.copy(isTouchGuardActive = !browserState.isTouchGuardActive)
                        },
                        onClose = { stopSelf() },
                        onCanGoBackChanged = { canGoBack -> browserState = browserState.copy(canGoBack = canGoBack) },
                        onWebViewReady = { webView -> webViewRef = webView },
                        onDragMove = ::onDragMove,
                        onDragResize = ::onDragResize,
                    )
                }
            }
        }
        composeView = view

        runCatching { wm.addView(view, params) }
            .onFailure {
                fireAndForgetLog("Floating Browser: addView thất bại — ${it.message}")
                showToast("Không thể hiển thị popup (${it.message ?: "lỗi không rõ"}).")
            }
    }

    private fun navigate(rawUrl: String) {
        val normalized = normalizeUrl(rawUrl)
        browserState = browserState.copy(currentUrl = normalized, addressBarText = normalized)
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(" ") || !trimmed.contains(".") ->
                "https://www.google.com/search?q=${Uri.encode(trimmed)}"
            else -> "https://$trimmed"
        }
    }

    private fun toggleMinimize() {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return

        if (!browserState.isMinimized) {
            lastExpandedWidthPx = params.width
            lastExpandedHeightPx = params.height
            params.width = (BUBBLE_SIZE_DP * resources.displayMetrics.density).roundToInt()
            params.height = params.width
        } else {
            params.width = lastExpandedWidthPx
            params.height = lastExpandedHeightPx
        }
        browserState = browserState.copy(isMinimized = !browserState.isMinimized)
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun cycleAlpha() {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return

        val levels = FloatingBrowserState.ALPHA_LEVELS
        val nextIndex = (levels.indexOf(browserState.alpha) + 1).let { if (it >= levels.size) 0 else it }
        val nextAlpha = levels[nextIndex]
        params.alpha = nextAlpha
        browserState = browserState.copy(alpha = nextAlpha)
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun onDragMove(dxPx: Float, dyPx: Float) {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        params.x += dxPx.roundToInt()
        params.y += dyPx.roundToInt()
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun onDragResize(dxPx: Float, dyPx: Float) {
        if (browserState.isMinimized) return
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        val density = resources.displayMetrics.density

        params.width = (params.width + dxPx.roundToInt()).coerceIn((MIN_WIDTH_DP * density).roundToInt(), (MAX_SIZE_DP * density).roundToInt())
        params.height = (params.height + dyPx.roundToInt()).coerceIn((MIN_HEIGHT_DP * density).roundToInt(), (MAX_SIZE_DP * density).roundToInt())
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, GameSpaceApp.CHANNEL_OVERLAY)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Floating Browser đang chạy")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    private fun fireAndForgetLog(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logManager.log(LogCategory.SHELL_COMMAND, message)
        }
    }

    /** Xem ghi chú tương tự ở [OverlayService.showToast] — báo lỗi rõ ràng thay vì im lặng. */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        runCatching { composeView?.let { windowManager?.removeView(it) } }
        lifecycleOwner?.destroy()
        serviceScope?.cancel()
        serviceScope = null
        composeView = null
        windowManager = null
        layoutParams = null
        webViewRef = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2003
        private const val DEFAULT_WIDTH_DP = 340
        private const val DEFAULT_HEIGHT_DP = 520
        private const val MIN_WIDTH_DP = 220
        private const val MIN_HEIGHT_DP = 320
        private const val MAX_SIZE_DP = 900
        private const val BUBBLE_SIZE_DP = 52
        const val EXTRA_URL = "extra_url"

        fun start(context: Context, url: String? = null) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWebViewService::class.java))
        }
    }
}
