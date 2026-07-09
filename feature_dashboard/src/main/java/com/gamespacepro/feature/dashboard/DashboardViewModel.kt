package com.gamespacepro.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespacepro.domain.accessibility.AccessibilityCapabilityDetector
import com.gamespacepro.domain.shell.ShellCapabilityDetector
import com.gamespacepro.domain.shell.ShellPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notably, this class depends only on `com.gamespacepro.domain.*`
 * interfaces — not on `feature_shizuku` or `feature_accessibility`
 * directly. Their concrete bindings are wired at the `:app` composition
 * root, which already depends on both. This module doesn't need to.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shellCapabilityDetector: ShellCapabilityDetector,
    private val shellPermissionManager: ShellPermissionManager,
    private val accessibilityCapabilityDetector: AccessibilityCapabilityDetector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            shellCapability = shellCapabilityDetector.currentCapability(),
            accessibilityCapability = accessibilityCapabilityDetector.currentCapability(),
        ),
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * No auto-refresh on resume yet (e.g. after the user grants Shizuku
     * permission or enables Accessibility in Settings and returns to the
     * app) — that needs a lifecycle-aware effect in the UI layer, deferred
     * as a UX polish pass rather than bundled in here. For now the user
     * taps the refresh action.
     */
    fun refreshCapabilities() {
        _uiState.update {
            it.copy(
                shellCapability = shellCapabilityDetector.currentCapability(),
                accessibilityCapability = accessibilityCapabilityDetector.currentCapability(),
            )
        }
    }

    fun onRequestShellPermissionClick() {
        // Must be set synchronously, before launching — not inside the
        // coroutine below. Otherwise two rapid clicks both read
        // isRequestingShellPermission=false before either has a chance to
        // flip it, and both proceed to request permission.
        if (_uiState.value.isRequestingShellPermission) return
        _uiState.update { it.copy(isRequestingShellPermission = true) }

        viewModelScope.launch {
            shellPermissionManager.requestPermission()
            _uiState.update { it.copy(isRequestingShellPermission = false) }
            refreshCapabilities()
        }
    }
}
