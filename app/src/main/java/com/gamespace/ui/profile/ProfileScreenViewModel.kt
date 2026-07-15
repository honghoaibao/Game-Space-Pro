package com.gamespace.ui.profile

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.automation.GameDetectionService
import com.gamespace.hardware.DeviceInfo
import com.gamespace.hardware.HardwareDetector
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

data class ProfileScreenUiState(
    val deviceInfo: DeviceInfo? = null,
    val activeProfile: ProfileType = ProfileType.BALANCED,
    val isApplyingProfile: Boolean = false,
    val lastApplyResult: ProfileApplyResult? = null,
    val isTrackingEnabled: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val hasShizukuPermission: Boolean = false,
)

/**
 * "Hồ sơ": chọn Performance Profile (quyết định giới hạn tần số quét/độ phân giải theo máy —
 * xem [ProfileEngine]/[com.gamespace.profile.ProfileConfig]) + bật/tắt theo dõi thời gian chơi.
 *
 * QUYẾT ĐỊNH THIẾT KẾ (ghi lại vì không nằm trong yêu cầu gốc của người dùng): công tắc "Tự động
 * tối ưu & theo dõi thời gian chơi" đặt ở đây thay vì có tab/nút riêng — vì [GameDetectionService]
 * (nền tảng ghi nhận dữ liệu cho tab "Thời gian chơi") CẦN được bật thì tab đó mới có dữ liệu, và
 * đây là màn hình duy nhất còn lại thuộc phạm vi "cài đặt cho phần chơi game" sau khi gỡ Dashboard/
 * AutomationCard cũ khỏi UI.
 */
@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareDetector: HardwareDetector,
    private val profileEngine: ProfileEngine,
    private val profileRepository: ProfileRepository,
    private val permissionChecker: PermissionChecker,
    private val recoveryManager: RecoveryManager,
    private val capabilityDetector: CapabilityDetector,
) : ViewModel() {

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        refresh()
    }

    private val deviceInfoFlow = MutableStateFlow<DeviceInfo?>(null)
    private val isApplyingFlow = MutableStateFlow(false)
    private val lastResultFlow = MutableStateFlow<ProfileApplyResult?>(null)
    private val isTrackingFlow = MutableStateFlow(false)
    private val hasUsageAccessFlow = MutableStateFlow(false)
    private val hasShizukuFlow = MutableStateFlow(false)

    private data class ProfilePart(
        val deviceInfo: DeviceInfo?,
        val activeProfile: ProfileType,
        val isApplying: Boolean,
        val lastResult: ProfileApplyResult?,
    )

    private data class TrackingPart(
        val isTracking: Boolean,
        val hasUsageAccess: Boolean,
        val hasShizuku: Boolean,
    )

    private val profilePartFlow = combine(
        deviceInfoFlow,
        profileRepository.activeProfile,
        isApplyingFlow,
        lastResultFlow,
    ) { deviceInfo, activeProfile, isApplying, lastResult ->
        ProfilePart(deviceInfo, activeProfile, isApplying, lastResult)
    }

    private val trackingPartFlow = combine(
        isTrackingFlow,
        hasUsageAccessFlow,
        hasShizukuFlow,
    ) { isTracking, hasUsageAccess, hasShizuku ->
        TrackingPart(isTracking, hasUsageAccess, hasShizuku)
    }

    val uiState: StateFlow<ProfileScreenUiState> = combine(
        profilePartFlow,
        trackingPartFlow,
    ) { profile, tracking ->
        ProfileScreenUiState(
            deviceInfo = profile.deviceInfo,
            activeProfile = profile.activeProfile,
            isApplyingProfile = profile.isApplying,
            lastApplyResult = profile.lastResult,
            isTrackingEnabled = tracking.isTracking,
            hasUsageAccessPermission = tracking.hasUsageAccess,
            hasShizukuPermission = tracking.hasShizuku,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileScreenUiState())

    init {
        refresh()
        capabilityDetector.addPermissionResultListener(shizukuPermissionListener)
        viewModelScope.launch { isTrackingFlow.value = recoveryManager.isAutomationEnabled.first() }
    }

    override fun onCleared() {
        capabilityDetector.removePermissionResultListener(shizukuPermissionListener)
        super.onCleared()
    }

    /** Gọi lại khi màn hình resume (vd. sau khi người dùng quay lại từ Settings cấp quyền). */
    fun refresh() {
        viewModelScope.launch { deviceInfoFlow.value = hardwareDetector.detect() }
        hasUsageAccessFlow.value = permissionChecker.hasUsageAccessPermission()
        hasShizukuFlow.value = capabilityDetector.isShizukuPermissionGranted()
    }

    fun selectProfile(type: ProfileType) {
        viewModelScope.launch {
            isApplyingFlow.value = true
            profileRepository.setActiveProfile(type)
            lastResultFlow.value = profileEngine.apply(type)
            isApplyingFlow.value = false
        }
    }

    fun requestShizukuPermission() = capabilityDetector.requestPermission()

    fun usageAccessIntent(): Intent = permissionChecker.usageAccessIntent()

    /** Bật/tắt [GameDetectionService] — nguồn dữ liệu duy nhất cho tab "Thời gian chơi". */
    fun toggleTracking() {
        refresh()
        if (!hasUsageAccessFlow.value) return // UI nên hiện nút xin quyền thay vì gọi hàm này

        val next = !isTrackingFlow.value
        if (next) GameDetectionService.start(context) else GameDetectionService.stop(context)
        isTrackingFlow.value = next
        viewModelScope.launch { recoveryManager.setAutomationEnabled(next) }
    }
}
