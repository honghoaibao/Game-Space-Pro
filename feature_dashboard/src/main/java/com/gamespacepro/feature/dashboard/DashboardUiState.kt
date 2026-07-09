package com.gamespacepro.feature.dashboard

import com.gamespacepro.domain.accessibility.AccessibilityCapability
import com.gamespacepro.domain.shell.ShellCapability

data class DashboardUiState(
    val shellCapability: ShellCapability,
    val accessibilityCapability: AccessibilityCapability,
    val isRequestingShellPermission: Boolean = false,
)
