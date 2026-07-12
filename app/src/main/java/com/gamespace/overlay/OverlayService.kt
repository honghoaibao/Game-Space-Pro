package com.gamespace.overlay

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.gamespace.optimizer.OptimizerEngine
import com.gamespace.permissions.PermissionChecker
import com.gamespace.profile.ProfileEngine
import com.gamespace.profile.ProfileRepository
import com.gamespace.profile.ProfileType
import com.gamespace.ui.theme.GameSpaceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground Service cho Floating HUD (mục "3. Game Space Overlay"). Vẽ overlay bằng
 * `WindowManager` + `ComposeView` (không có Activity — dùng [OverlayLifecycleOwner]
 * để cấp phát Lifecycle/ViewModelStore/SavedStateRegistry thủ công).
 *
 * Yêu cầu quyền `SYSTEM_ALERT_WINDOW` (Settings.canDrawOverlays) — nếu chưa cấp,
 * service tự dừng ngay (không crash, xem [PermissionChecker]).
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var permissionChecker: PermissionChecker

    @Inject lateinit var metricsCollector: HudMetricsCollector

    @Inject lateinit var optimizerEngine: OptimizerEngine

    @Inject lateinit var profileEngine: ProfileEngine

    @Inject lateinit var profileRepository: ProfileRepository

    @Inject lateinit var logManager: LogManager

    private var serviceScope: CoroutineScope? = null
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var timerJob: Job? = null

    /** Package của game đang theo dõi — do [com.gamespace.automation.GameDetectionService] truyền vào. */
    private var currentGamePackage: String? = null

    private var metricsState by mutableStateOf(HudMetrics())
    private var isExpandedState by mutableStateOf(true)
    private var isDndActiveState by mutableStateOf(false)
    private var timerRemainingState by mutableIntStateOf(-1) // -1 = không chạy

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentGamePackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE) ?: currentGamePackage
        startForeground(NOTIFICATION_ID, buildNotification())

        if (composeView == null) {
            if (!permissionChecker.hasOverlayPermission()) {
                fireAndForgetLog("Overlay: thiếu quyền SYSTEM_ALERT_WINDOW — dừng service")
                showToast("Chưa cấp quyền hiển thị popup — cấp quyền \"Hiển thị trên ứng dụng khác\" ở Dashboard rồi thử lại.")
                stopSelf()
                return START_NOT_STICKY
            }
            setupOverlay()
        }
        return START_STICKY
    }

    private fun setupOverlay() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        serviceScope = scope

        val owner = OverlayLifecycleOwner().apply {
            performRestore()
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 200
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                GameSpaceTheme(darkTheme = true) {
                    HudContent(
                        metrics = metricsState,
                        isExpanded = isExpandedState,
                        isDndActive = isDndActiveState,
                        timerRemainingSeconds = timerRemainingState.takeIf { it >= 0 },
                        onToggleExpand = { isExpandedState = !isExpandedState },
                        onDeepClean = { scope.launch { optimizerEngine.deepRamClean() } },
                        onToggleDnd = ::toggleDnd,
                        onCycleProfile = { scope.launch { cycleProfile() } },
                        onToggleTimer = ::toggleTimer,
                        onDrag = ::onDrag,
                    )
                }
            }
        }
        composeView = view

        runCatching { wm.addView(view, params) }
            .onFailure {
                fireAndForgetLog("Overlay: addView thất bại — ${it.message}")
                showToast("Không thể hiển thị popup HUD (${it.message ?: "lỗi không rõ"}).")
            }

        metricsCollector.metricsFlow()
            .onEach { metricsState = it }
            .launchIn(scope)
    }

    private fun onDrag(dxPx: Float, dyPx: Float) {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        params.x += dxPx.toInt()
        params.y += dyPx.toInt()
        runCatching { wm.updateViewLayout(view, params) }
    }

    private suspend fun cycleProfile() {
        val next = when (profileRepository.activeProfile.first()) {
            ProfileType.LOW -> ProfileType.BALANCED
            ProfileType.BALANCED -> ProfileType.PERFORMANCE
            ProfileType.PERFORMANCE -> ProfileType.LOW
        }
        profileRepository.setActiveProfile(next)
        profileEngine.apply(next, currentGamePackage)
    }

    private fun toggleDnd() {
        if (!permissionChecker.hasNotificationPolicyAccess()) {
            startActivity(
                permissionChecker.notificationPolicyIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val nextFilter = if (isDndActiveState) {
            NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        runCatching { manager.setInterruptionFilter(nextFilter) }
            .onSuccess { isDndActiveState = !isDndActiveState }
    }

    private fun toggleTimer() {
        if (timerJob?.isActive == true) {
            timerJob?.cancel()
            timerJob = null
            timerRemainingState = -1
            return
        }
        val scope = serviceScope ?: return
        timerRemainingState = DEFAULT_TIMER_SECONDS
        timerJob = scope.launch {
            while (timerRemainingState > 0) {
                delay(1_000)
                timerRemainingState -= 1
            }
            timerRemainingState = -1
            fireAndForgetLog("Tactical Timer kết thúc")
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, GameSpaceApp.CHANNEL_OVERLAY)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Overlay HUD đang chạy" + (currentGamePackage?.let { " · $it" } ?: ""))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    /** Log không phụ thuộc `serviceScope` (có thể null lúc gọi sớm, vd. khi thiếu quyền overlay). */
    private fun fireAndForgetLog(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logManager.log(LogCategory.SHELL_COMMAND, message)
        }
    }

    /**
     * Trước đây các lỗi (thiếu quyền, addView thất bại) chỉ được ghi vào Log — người dùng bấm
     * "Mở HUD" không thấy gì xảy ra và không hiểu vì sao (báo cáo lỗi: "popup không hiển thị").
     * Toast hiển thị ngay lý do, kể cả khi service chạy nền không có Activity nào đang mở.
     */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        runCatching { composeView?.let { windowManager?.removeView(it) } }
        lifecycleOwner?.destroy()
        serviceScope?.cancel()
        serviceScope = null
        composeView = null
        windowManager = null
        layoutParams = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val DEFAULT_TIMER_SECONDS = 10 * 60
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"

        fun start(context: Context, targetPackage: String?) {
            val intent = Intent(context, OverlayService::class.java).apply {
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
