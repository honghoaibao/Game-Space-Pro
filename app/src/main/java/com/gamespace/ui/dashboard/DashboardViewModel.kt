package com.gamespace.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.automation.GameDetectionService
import com.gamespace.hardware.DeviceInfo
import com.gamespace.hardware.HardwareDetector
import com.gamespace.optimizer.OptimizeResult
import com.gamespace.optimizer.OptimizerEngine
import com.gamespace.permissions.PermissionChecker
import com.gamespace.profile.ProfileApplyResult
import com.gamespace.profile.ProfileEngine
import com.gamespace.profile.ProfileRepository
import com.gamespace.profile.ProfileType
import com.gamespace.recovery.RecoveryManager
import com.gamespace.shizuku.CapabilityDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

data class DashboardUiState(
    val deviceInfo: DeviceInfo? = null,
    val activeProfile: ProfileType = ProfileType.BALANCED,
    val isApplyingProfile: Boolean = false,
    val lastApplyResult: ProfileApplyResult? = null,
    val isOptimizing: Boolean = false,
    val lastOptimizeResult: OptimizeResult? = null,
    val isAutomationRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasBatteryOptimizationExemption: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareDetector: HardwareDetector,
    private val profileEngine: ProfileEngine,
    private val profileRepository: ProfileRepository,
    private val optimizerEngine: OptimizerEngine,
    private val permissionChecker: PermissionChecker,
    private val recoveryManager: RecoveryManager,
    private val capabilityDetector: CapabilityDetector,
) : ViewModel() {

    private data class PermissionsSnapshot(
        val overlay: Boolean = false,
        val usageAccess: Boolean = false,
        val accessibility: Boolean = false,
        val batteryOptimizationExempt: Boolean = false,
    )

    /** Cập nhật UI ngay khi người dùng bấm Allow/Deny trên dialog quyền của Shizuku. */
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        refreshDeviceInfo()
    }

    private val deviceInfoFlow = MutableStateFlow<DeviceInfo?>(null)
    private val isApplyingFlow = MutableStateFlow(false)
    private val lastResultFlow = MutableStateFlow<ProfileApplyResult?>(null)
    private val isOptimizingFlow = MutableStateFlow(false)
    private val lastOptimizeResultFlow = MutableStateFlow<OptimizeResult?>(null)
    private val isAutomationRunningFlow = MutableStateFlow(false)
    private val permissionsFlow = MutableStateFlow(PermissionsSnapshot())

    private data class ProfileState(
        val deviceInfo: DeviceInfo?,
        val activeProfile: ProfileType,
        val isApplying: Boolean,
        val lastResult: ProfileApplyResult?,
    )

    private data class OptimizeState(
        val isOptimizing: Boolean,
        val lastResult: OptimizeResult?,
    )

    private data class AutomationState(
        val isRunning: Boolean,
        val permissions: PermissionsSnapshot,
    )

    private val profileStateFlow = combine(
        deviceInfoFlow,
        profileRepository.activeProfile,
        isApplyingFlow,
        lastResultFlow,
    ) { deviceInfo, activeProfile, isApplying, lastResult ->
        ProfileState(deviceInfo, activeProfile, isApplying, lastResult)
    }

    private val optimizeStateFlow = combine(isOptimizingFlow, lastOptimizeResultFlow) { isOptimizing, lastResult ->
        OptimizeState(isOptimizing, lastResult)
    }

    private val automationStateFlow = combine(isAutomationRunningFlow, permissionsFlow) { isRunning, permissions ->
        AutomationState(isRunning, permissions)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        profileStateFlow,
        optimizeStateFlow,
        automationStateFlow,
    ) { profile, optimize, automation ->
        DashboardUiState(
            deviceInfo = profile.deviceInfo,
            activeProfile = profile.activeProfile,
            isApplyingProfile = profile.isApplying,
            lastApplyResult = profile.lastResult,
            isOptimizing = optimize.isOptimizing,
            lastOptimizeResult = optimize.lastResult,
            isAutomationRunning = automation.isRunning,
            hasOverlayPermission = automation.permissions.overlay,
            hasUsageAccessPermission = automation.permissions.usageAccess,
            hasAccessibilityPermission = automation.permissions.accessibility,
            hasBatteryOptimizationExemption = automation.permissions.batteryOptimizationExempt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init {
        refreshDeviceInfo()
        refreshPermissions()
        capabilityDetector.addPermissionResultListener(shizukuPermissionListener)
        viewModelScope.launch {
            // Best-effort: phản ánh ý định bật/tắt Automation của người dùng từ phiên trước
            // (không đảm bảo service thực sự vẫn đang chạy — chỉ là gợi ý UI, xem RecoveryManager).
            isAutomationRunningFlow.value = recoveryManager.isAutomationEnabled.first()
        }
    }

    override fun onCleared() {
        capabilityDetector.removePermissionResultListener(shizukuPermissionListener)
        super.onCleared()
    }

    fun refreshDeviceInfo() {
        viewModelScope.launch {
            deviceInfoFlow.value = hardwareDetector.detect()
        }
    }

    /** Gọi lại mỗi khi Dashboard resume (vd. sau khi người dùng quay lại từ màn Settings cấp quyền). */
    fun refreshPermissions() {
        permissionsFlow.value = PermissionsSnapshot(
            overlay = permissionChecker.hasOverlayPermission(),
            usageAccess = permissionChecker.hasUsageAccessPermission(),
            accessibility = permissionChecker.hasAccessibilityEnabled(),
            batteryOptimizationExempt = permissionChecker.hasIgnoreBatteryOptimizations(),
        )
    }

    /** Mở dialog cấp quyền của chính Shizuku (chỉ có tác dụng khi Shizuku đã cài & đang chạy). */
    fun requestShizukuPermission() = capabilityDetector.requestPermission()

    fun selectProfile(type: ProfileType, targetPackage: String? = null) {
        viewModelScope.launch {
            isApplyingFlow.value = true
            profileRepository.setActiveProfile(type)
            val result = profileEngine.apply(type, targetPackage)
            lastResultFlow.value = result
            isApplyingFlow.value = false
        }
    }

    fun deepRamClean() = runOptimize { optimizerEngine.deepRamClean() }

    fun clearCache() = runOptimize { optimizerEngine.clearCache() }

    fun storageTrim() = runOptimize { optimizerEngine.storageTrim() }

    private fun runOptimize(action: suspend () -> OptimizeResult) {
        viewModelScope.launch {
            isOptimizingFlow.value = true
            lastOptimizeResultFlow.value = action()
            isOptimizingFlow.value = false
            refreshDeviceInfo() // cập nhật lại RAM khả dụng sau khi tối ưu
        }
    }

    fun overlayPermissionIntent() = permissionChecker.overlayPermissionIntent()

    fun usageAccessIntent() = permissionChecker.usageAccessIntent()

    fun accessibilitySettingsIntent() = permissionChecker.accessibilitySettingsIntent()

    fun ignoreBatteryOptimizationsIntent() = permissionChecker.ignoreBatteryOptimizationsIntent()

    /** Null nếu OEM hiện tại không có màn "Tự khởi động" nhận diện được — UI nên ẩn nút trong TH này. */
    fun oemAutoStartIntent() = permissionChecker.oemAutoStartIntent()

    fun appDetailsSettingsIntent() = permissionChecker.appDetailsSettingsIntent()

    fun toggleAutomation() {
        refreshPermissions()
        if (!permissionsFlow.value.usageAccess) return // UI nên hiển thị nút xin quyền thay vì gọi hàm này

        val nextRunning = !isAutomationRunningFlow.value
        if (nextRunning) {
            GameDetectionService.start(context)
        } else {
            GameDetectionService.stop(context)
        }
        isAutomationRunningFlow.value = nextRunning
        viewModelScope.launch { recoveryManager.setAutomationEnabled(nextRunning) }
        // Lưu ý: Overlay/Accessibility không bắt buộc để bật Automation (Profile/dọn nền vẫn
        // hoạt động), chỉ ảnh hưởng tới HUD hiển thị hay độ chính xác phát hiện game.
    }
}
