package com.gamespace.diagnostics

import android.content.Context
import androidx.core.content.FileProvider
import com.gamespace.hardware.HardwareDetector
import com.gamespace.permissions.PermissionChecker
import com.gamespace.profile.ProfileRepository
import com.gamespace.recovery.RecoveryManager
import com.gamespace.shizuku.CapabilityDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Mục "10. Diagnostics" — gom mọi trạng thái (phần cứng, quyền, Shizuku, Automation)
 * thành một báo cáo duy nhất, xuất ra file `.txt` chia sẻ được qua `ACTION_SEND`
 * (dùng chung cơ chế `FileProvider` với [com.gamespace.logging.LogManager]).
 */
@Singleton
class DiagnosticsGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareDetector: HardwareDetector,
    private val profileRepository: ProfileRepository,
    private val permissionChecker: PermissionChecker,
    private val capabilityDetector: CapabilityDetector,
    private val recoveryManager: RecoveryManager,
) {

    suspend fun generate(): DiagnosticsReport = DiagnosticsReport(
        generatedAtMillis = System.currentTimeMillis(),
        deviceInfo = hardwareDetector.detect(),
        activeProfile = profileRepository.activeProfile.first(),
        hasOverlayPermission = permissionChecker.hasOverlayPermission(),
        hasUsageAccessPermission = permissionChecker.hasUsageAccessPermission(),
        hasAccessibilityPermission = permissionChecker.hasAccessibilityEnabled(),
        hasNotificationPolicyAccess = permissionChecker.hasNotificationPolicyAccess(),
        hasPostNotificationPermission = permissionChecker.hasPostNotificationPermission(),
        isAutomationEnabled = recoveryManager.isAutomationEnabled.first(),
    )

    fun formatAsText(report: DiagnosticsReport): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val info = report.deviceInfo
        return buildString {
            appendLine("GAME SPACE — Báo cáo chẩn đoán")
            appendLine("Tạo lúc: ${formatter.format(Date(report.generatedAtMillis))}")
            appendLine("=".repeat(48))
            appendLine("[Thiết bị]")
            appendLine("Model: ${info.manufacturer} ${info.model}")
            appendLine("Android: ${info.androidVersion} (API ${info.apiLevel})")
            appendLine("ABI: ${info.supportedAbis.joinToString()}")
            appendLine("CPU: ${info.cpuCoreCount} nhân")
            appendLine("RAM: ${"%.1f".format(info.availableRamGb)} / ${"%.1f".format(info.totalRamGb)} GB khả dụng")
            appendLine("Màn hình: ${info.screenWidthPx}x${info.screenHeightPx} @ ${info.currentRefreshRate}Hz")
            appendLine("Refresh rate hỗ trợ: ${info.supportedRefreshRates.joinToString()}")
            appendLine("Game Mode API: ${info.supportsGameMode}")
            appendLine("Thermal API: ${info.supportsThermalApi}")
            appendLine("Low RAM Device: ${info.isLowRamDevice}")
            appendLine()
            appendLine("[Profile]")
            appendLine("Đang áp dụng: ${report.activeProfile.name}")
            appendLine("Gợi ý theo phần cứng: ${info.suggestedProfile().name}")
            appendLine()
            appendLine("[Quyền]")
            appendLine("Shizuku cài đặt: ${info.shizukuInstalled}")
            appendLine("Shizuku đã cấp quyền: ${info.shizukuGranted}")
            appendLine("Overlay: ${report.hasOverlayPermission}")
            appendLine("Usage Access: ${report.hasUsageAccessPermission}")
            appendLine("Accessibility: ${report.hasAccessibilityPermission}")
            appendLine("Notification Policy (DND): ${report.hasNotificationPolicyAccess}")
            appendLine("Post Notification: ${report.hasPostNotificationPermission}")
            appendLine()
            appendLine("[Automation]")
            appendLine("Đã bật (lần gần nhất): ${report.isAutomationEnabled}")
            appendLine()
            appendLine("[Capability Matrix — Shizuku]")
            appendLine("Chạy lệnh shell: ${capabilityDetector.canRunShellCommands()}")
            appendLine("Force-stop app khác: ${capabilityDetector.canForceStopOtherApps()}")
            appendLine("Đổi refresh rate hệ thống: ${capabilityDetector.canOverrideSystemRefreshRate()}")
            appendLine("Đổi độ phân giải hệ thống: ${capabilityDetector.canOverrideResolution()}")
            appendLine("Chạy fstrim: ${capabilityDetector.canRunFstrim()}")
        }
    }

    suspend fun exportToFile(report: DiagnosticsReport = generate()): android.net.Uri {
        val text = formatAsText(report)
        val exportDir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val file = File(exportDir, "gamespace_diagnostics_${System.currentTimeMillis()}.txt")
        file.writeText(text)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
