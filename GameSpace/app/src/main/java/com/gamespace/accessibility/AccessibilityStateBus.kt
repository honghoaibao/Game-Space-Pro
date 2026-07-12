package com.gamespace.accessibility

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cầu nối dữ liệu giữa [GameSpaceAccessibilityService] (chỉ chạy khi người dùng tự bật
 * trong Settings, không đảm bảo luôn có) và phần còn lại của app. Đây là dữ liệu
 * "bổ sung" — [com.gamespace.automation.GameDetectionService] không phụ thuộc vào đây
 * để hoạt động (nguồn chính là `UsageStatsManager`), Accessibility chỉ giúp phát hiện
 * nhanh hơn + nhận diện dialog mà UsageStatsManager không làm được.
 */
@Singleton
class AccessibilityStateBus @Inject constructor() {
    private val foregroundPackageFlow = MutableStateFlow<String?>(null)
    private val dialogShowingFlow = MutableStateFlow(false)
    private val serviceConnectedFlow = MutableStateFlow(false)

    val foregroundPackage: StateFlow<String?> = foregroundPackageFlow.asStateFlow()
    val isDialogShowing: StateFlow<Boolean> = dialogShowingFlow.asStateFlow()
    val isServiceConnected: StateFlow<Boolean> = serviceConnectedFlow.asStateFlow()

    fun updateForegroundPackage(packageName: String?) {
        foregroundPackageFlow.value = packageName
    }

    fun updateDialogShowing(isShowing: Boolean) {
        dialogShowingFlow.value = isShowing
    }

    fun updateServiceConnected(isConnected: Boolean) {
        serviceConnectedFlow.value = isConnected
    }
}
