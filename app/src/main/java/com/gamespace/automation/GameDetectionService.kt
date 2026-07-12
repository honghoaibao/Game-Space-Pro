package com.gamespace.automation

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gamespace.GameSpaceApp
import com.gamespace.R
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.optimizer.OptimizerEngine
import com.gamespace.overlay.OverlayService
import com.gamespace.packagemanager.GameRepository
import com.gamespace.permissions.PermissionChecker
import com.gamespace.profile.ProfileEngine
import com.gamespace.profile.ProfileRepository
import com.gamespace.profile.ProfileType
import com.gamespace.recovery.RecoveryManager
import com.gamespace.thermal.ThermalGuard
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground Service nền cho "4. Smart Automation" + "5. Thermal Protection" (ADR-005:
 * Thermal Guard chạy chung vòng đời với Automation Service).
 *
 * Phát hiện game foreground bằng polling nhẹ qua `UsageStatsManager` (không dùng
 * AccessibilityService — để tính năng lõi không phụ thuộc quyền tùy chọn ở Phiên 4).
 * Khi vào game đã theo dõi: dọn nền, áp Profile (riêng của game hoặc Profile hệ thống
 * hiện tại), mở Overlay HUD. Khi thoát: ghi nhận thời gian chơi, đóng Overlay, khôi
 * phục Profile trước đó.
 */
@AndroidEntryPoint
class GameDetectionService : Service() {

    @Inject lateinit var permissionChecker: PermissionChecker

    @Inject lateinit var gameRepository: GameRepository

    @Inject lateinit var profileEngine: ProfileEngine

    @Inject lateinit var profileRepository: ProfileRepository

    @Inject lateinit var optimizerEngine: OptimizerEngine

    @Inject lateinit var thermalGuard: ThermalGuard

    @Inject lateinit var recoveryManager: RecoveryManager

    @Inject lateinit var logManager: LogManager

    private var serviceScope: CoroutineScope? = null
    private var trackedPackages: Set<String> = emptySet()
    private var currentGamePackage: String? = null
    private var sessionStartMillis: Long = 0
    private var preSessionProfile: ProfileType? = null

    /**
     * Mốc thời gian bắt đầu KHÔNG thấy game đang theo dõi ở foreground nữa, hoặc `null` nếu
     * đang thấy game bình thường. Dùng để tạo "grace period" trước khi coi là thoát game thật
     * (xem [EXIT_GRACE_MILLIS]) — trước đây thoát ngay lập tức khiến Overlay HUD/dọn nền bị
     * kích hoạt nhầm mỗi khi có dialog hệ thống, thanh thông báo, bàn phím... thoáng qua che
     * foreground app trong 1 nhịp poll (báo cáo lỗi: "popup đôi khi biến mất").
     */
    private var awayFromGameSinceMillis: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(null))

        if (serviceScope == null) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            serviceScope = scope

            gameRepository.observeGames()
                .onEach { games -> trackedPackages = games.map { it.packageName }.toSet() }
                .launchIn(scope)

            scope.launch { thermalGuard.run() }
            scope.launch { pollLoop() }
        }
        return START_STICKY
    }

    private suspend fun pollLoop() {
        if (!permissionChecker.hasUsageAccessPermission()) {
            logManager.log(LogCategory.ERROR, "Automation: thiếu quyền Usage Access — dừng service")
            stopSelf()
            return
        }
        while (true) {
            handleForeground(readForegroundPackage())
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    /** Đọc package đang foreground trong [WINDOW_MILLIS] gần nhất qua UsageEvents. */
    private fun readForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val end = System.currentTimeMillis()
        val begin = end - WINDOW_MILLIS
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForegroundEvent = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            if (isForegroundEvent) lastForegroundPackage = event.packageName
        }
        return lastForegroundPackage
    }

    private suspend fun handleForeground(foregroundPackage: String?) {
        val isTrackedGame = foregroundPackage != null && foregroundPackage in trackedPackages

        when {
            isTrackedGame && foregroundPackage != currentGamePackage -> {
                awayFromGameSinceMillis = null
                if (currentGamePackage != null) endSession()
                startSession(requireNotNull(foregroundPackage))
            }
            isTrackedGame && foregroundPackage == currentGamePackage -> {
                // Vẫn đang trong game đã theo dõi — hủy đếm ngược thoát nếu vừa mới có 1-2 nhịp
                // poll bị che (dialog/thông báo/bàn phím) rồi quay lại ngay.
                awayFromGameSinceMillis = null
            }
            !isTrackedGame && currentGamePackage != null -> {
                val awaySince = awayFromGameSinceMillis
                    ?: System.currentTimeMillis().also { awayFromGameSinceMillis = it }
                if (System.currentTimeMillis() - awaySince >= EXIT_GRACE_MILLIS) {
                    endSession()
                    awayFromGameSinceMillis = null
                }
            }
        }
    }

    private suspend fun startSession(packageName: String) {
        currentGamePackage = packageName
        sessionStartMillis = System.currentTimeMillis()
        preSessionProfile = profileRepository.activeProfile.first()

        logManager.log(LogCategory.GAME_LAUNCH, "Phát hiện game mở: $packageName")
        optimizerEngine.runFullOptimize()

        val assignedProfile = gameRepository.getGame(packageName)?.assignedProfile
        val profileToApply = assignedProfile ?: preSessionProfile ?: ProfileType.BALANCED
        profileRepository.setActiveProfile(profileToApply)
        profileEngine.apply(profileToApply, packageName)

        if (permissionChecker.hasOverlayPermission()) {
            OverlayService.start(this, packageName)
        } else {
            logManager.log(LogCategory.GAME_LAUNCH, "Bỏ qua mở Overlay cho $packageName (thiếu quyền overlay)")
        }
        updateNotification(packageName)
    }

    private suspend fun endSession() {
        val packageName = currentGamePackage ?: return
        val durationMillis = System.currentTimeMillis() - sessionStartMillis
        gameRepository.recordSession(packageName, durationMillis)
        logManager.log(
            LogCategory.GAME_LAUNCH,
            "Thoát game: $packageName · đã chơi ${durationMillis / 60_000} phút",
        )

        // Chỉ tự tắt HUD nếu người dùng KHÔNG bật nó thủ công (nút "Mở HUD ngay" trên Dashboard,
        // xem DashboardViewModel.toggleHud) — tránh Automation vô tình đóng popup mà người dùng
        // đã tự mở tay, độc lập với việc theo dõi game.
        if (!recoveryManager.isHudEnabled.first()) {
            OverlayService.stop(this)
        }
        currentGamePackage = null

        preSessionProfile?.let { restored ->
            profileRepository.setActiveProfile(restored)
            profileEngine.apply(restored)
        }
        preSessionProfile = null
        updateNotification(null)
    }

    private fun updateNotification(activePackage: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(activePackage))
    }

    private fun buildNotification(activePackage: String?): Notification =
        NotificationCompat.Builder(this, GameSpaceApp.CHANNEL_AUTOMATION)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (activePackage != null) "Đang tối ưu cho: $activePackage" else "Đang theo dõi để tự động hóa",
            )
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2002

        /** Giảm từ 2000ms — phát hiện game mở nhanh hơn (bớt cảm giác "delay" khi mở popup). */
        private const val POLL_INTERVAL_MILLIS = 1_000L
        private const val WINDOW_MILLIS = 10_000L

        /**
         * Phải liên tục KHÔNG thấy game ở foreground trong khoảng này mới coi là thoát thật.
         * Che chắn các lần foreground bị "cướp" thoáng qua bởi dialog hệ thống, thanh thông
         * báo, bàn phím, quảng cáo overlay... — nguyên nhân khiến Overlay HUD từng bị tắt/mở
         * nhấp nháy dù người dùng vẫn đang ở trong game (báo cáo lỗi: "popup đôi khi biến mất").
         */
        private const val EXIT_GRACE_MILLIS = 4_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GameDetectionService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GameDetectionService::class.java))
        }
    }
}
