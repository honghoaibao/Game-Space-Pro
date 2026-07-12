package com.gamespace.diagnostics

import com.gamespace.hardware.DeviceInfo
import com.gamespace.profile.ProfileType

/**
 * Mục "10. Diagnostics" — tổng hợp toàn bộ thông tin cần để chẩn đoán sự cố hoặc
 * đính kèm khi người dùng báo lỗi.
 */
data class DiagnosticsReport(
    val generatedAtMillis: Long,
    val deviceInfo: DeviceInfo,
    val activeProfile: ProfileType,
    val hasOverlayPermission: Boolean,
    val hasUsageAccessPermission: Boolean,
    val hasAccessibilityPermission: Boolean,
    val hasNotificationPolicyAccess: Boolean,
    val hasPostNotificationPermission: Boolean,
    val isAutomationEnabled: Boolean,
)
