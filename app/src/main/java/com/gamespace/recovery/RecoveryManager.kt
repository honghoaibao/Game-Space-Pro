package com.gamespace.recovery

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.profile.ProfileRepository
import com.gamespace.profile.ProfileType
import com.gamespace.shizuku.CapabilityDetector
import com.gamespace.shizuku.ShellExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.recoveryDataStore by preferencesDataStore(name = "recovery_prefs")
private val KEY_CLEAN_SHUTDOWN = booleanPreferencesKey("clean_shutdown")
private val KEY_AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
private val KEY_HUD_ENABLED = booleanPreferencesKey("hud_enabled")

/**
 * Mục "12. Recovery" — dùng mẫu "dirty bit" chuẩn để phát hiện phiên trước có tắt sạch
 * hay không: [markSessionActive] đặt cờ về "chưa sạch" ngay khi app lên foreground,
 * [markCleanShutdown] đặt lại "sạch" khi app về nền một cách bình thường (qua
 * `ProcessLifecycleOwner`, không phải Activity — vì đây là trạng thái CẢ TIẾN TRÌNH).
 * Nếu app bị Crash / Force Close / Android Kill, [markCleanShutdown] sẽ không kịp chạy,
 * nên lần khởi động kế tiếp [checkAndRecoverIfNeeded] sẽ thấy cờ vẫn "chưa sạch" và biết
 * cần khôi phục.
 *
 * Khôi phục gồm: độ phân giải + mật độ điểm ảnh + tần số quét hệ thống (những thứ
 * [com.gamespace.profile.ProfileEngine] có thể đã đổi qua Shizuku và KHÔNG tự động
 * revert khi app chết — xem ADR-006/ADR-007 trong `ARCHITECTURE.md`), và Profile về
 * Balanced Mode (lựa chọn an toàn trung tính).
 */
@Singleton
class RecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDetector: CapabilityDetector,
    private val shellExecutor: ShellExecutor,
    private val profileRepository: ProfileRepository,
    private val logManager: LogManager,
) {

    val isAutomationEnabled: Flow<Boolean> =
        context.recoveryDataStore.data.map { it[KEY_AUTOMATION_ENABLED] ?: false }

    suspend fun setAutomationEnabled(enabled: Boolean) {
        context.recoveryDataStore.edit { it[KEY_AUTOMATION_ENABLED] = enabled }
    }

    /**
     * Trạng thái bật/tắt THỦ CÔNG của Floating HUD (nút "Mở HUD ngay" trên Dashboard —
     * độc lập với Smart Automation, xem [com.gamespace.ui.dashboard.DashboardViewModel.toggleHud]).
     * Chỉ là gợi ý UI cho lần mở app kế tiếp, giống hệt [isAutomationEnabled] — không đảm bảo
     * service thực sự vẫn đang chạy (vd. hệ thống có thể đã kill nền).
     */
    val isHudEnabled: Flow<Boolean> =
        context.recoveryDataStore.data.map { it[KEY_HUD_ENABLED] ?: false }

    suspend fun setHudEnabled(enabled: Boolean) {
        context.recoveryDataStore.edit { it[KEY_HUD_ENABLED] = enabled }
    }

    /** Gọi khi cả tiến trình lên foreground (ProcessLifecycleOwner ON_START). */
    suspend fun markSessionActive() {
        context.recoveryDataStore.edit { it[KEY_CLEAN_SHUTDOWN] = false }
    }

    /** Gọi khi cả tiến trình về nền một cách bình thường (ProcessLifecycleOwner ON_STOP). */
    suspend fun markCleanShutdown() {
        context.recoveryDataStore.edit { it[KEY_CLEAN_SHUTDOWN] = true }
    }

    /** Gọi một lần duy nhất ở cold start ([com.gamespace.GameSpaceApp.onCreate]). */
    suspend fun checkAndRecoverIfNeeded() {
        val prefs = context.recoveryDataStore.data.first()
        // Mặc định true (coi là "sạch") cho lần cài đặt đầu tiên để không log khôi phục giả.
        val wasCleanShutdown = prefs[KEY_CLEAN_SHUTDOWN] ?: true

        if (!wasCleanShutdown) {
            logManager.log(
                LogCategory.ERROR,
                "Phát hiện phiên trước không tắt sạch (crash/bị buộc dừng/hệ thống kill) — bắt đầu khôi phục",
            )
            recoverDisplaySettings()
            profileRepository.setActiveProfile(ProfileType.BALANCED)
            logManager.log(LogCategory.PROFILE, "Khôi phục Profile về Balanced Mode sau sự cố")
        }
    }

    private suspend fun recoverDisplaySettings() {
        if (!capabilityDetector.canOverrideResolution()) {
            logManager.log(LogCategory.ERROR, "Bỏ qua khôi phục độ phân giải/tần số quét (thiếu Shizuku)")
            return
        }
        // Cả 3 lệnh đều an toàn khi gọi lặp lại (idempotent) — không gây hại nếu vốn dĩ chưa từng đổi.
        shellExecutor.execute("wm size reset")
        shellExecutor.execute("wm density reset")
        shellExecutor.execute("settings delete system peak_refresh_rate")
        logManager.log(LogCategory.ERROR, "Đã khôi phục độ phân giải/mật độ/tần số quét hệ thống về mặc định")
    }
}
