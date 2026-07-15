package com.gamespace.permissions

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Permission Checker" — kiểm tra các quyền cấp cao (Overlay, Usage Access, DND,
 * Post Notification) mà Android bắt buộc người dùng cấp thủ công qua Settings
 * (không thể xin qua dialog runtime thông thường). Mọi module dùng các quyền
 * này (Overlay HUD, Automation) đều phải hỏi qua đây trước, tương tự
 * [com.gamespace.shizuku.CapabilityDetector] cho Shizuku (ADR-002).
 */
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Cần cho Overlay HUD / Floating WebView (`TYPE_APPLICATION_OVERLAY`). */
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun overlayPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Cần cho Smart Automation (phát hiện game foreground qua UsageStatsManager). */
    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** Cần cho Quick Tools "Chặn Notification" (Do Not Disturb). */
    fun hasNotificationPolicyAccess(): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return manager?.isNotificationPolicyAccessGranted == true
    }

    fun notificationPolicyIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /**
     * Miễn tối ưu pin CHO CHÍNH GAME SPACE (không phải cho game đích — xem
     * [com.gamespace.profile.ProfileEngine.applyBatteryOptimizationWhitelist] cho việc đó).
     * Cần để Foreground Service (Overlay HUD, Popup Apps, Automation) không bị hệ điều hành/OEM
     * tự dọn nền — thiếu quyền này là nguyên nhân phổ biến nhất khiến popup/overlay tự biến mất
     * dù Foreground Service + notification đã đúng.
     */
    fun hasIgnoreBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun ignoreBatteryOptimizationsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    /**
     * Best-effort: tìm màn hình "Tự khởi động / Quản lý chạy nền" riêng của OEM (Xiaomi, Oppo, Vivo,
     * Huawei...). Android không có API chuẩn cho việc này — mỗi hãng tự làm màn hình riêng và có thể
     * đổi package/activity giữa các bản ROM, nên KHÔNG đảm bảo đúng trên mọi máy. Luôn trả về null
     * nếu không có activity nào resolve được, để UI ẩn nút thay vì mở ra màn hình lỗi.
     */
    fun oemAutoStartIntent(): Intent? {
        val candidates = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> listOf(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            )
            "oppo", "realme" -> listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            )
            "vivo" -> listOf(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            )
            "huawei", "honor" -> listOf(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                ),
            )
            else -> emptyList()
        }
        return candidates
            .map { Intent().apply { component = it; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } }
            .firstOrNull { context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }
    }

    /** Fallback khi [oemAutoStartIntent] trả về null hoặc chính activity đó mở lỗi lúc runtime. */
    fun appDetailsSettingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** Android 13+ (API 33) yêu cầu quyền runtime để hiển thị Notification (Foreground Service). */
    fun hasPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Accessibility Engine (mục "8.") — tùy chọn, người dùng tự bật trong Settings.
     * Không có API kiểm tra trực tiếp; đọc danh sách service đã bật từ Settings.Secure
     * (cách chuẩn được Google khuyến nghị để kiểm tra service của chính app).
     */
    fun hasAccessibilityEnabled(): Boolean {
        val expectedComponent = "${context.packageName}/$ACCESSIBILITY_SERVICE_CLASS_NAME"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    companion object {
        private const val ACCESSIBILITY_SERVICE_CLASS_NAME =
            "com.gamespace.accessibility.GameSpaceAccessibilityService"
    }
}
