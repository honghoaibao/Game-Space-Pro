package com.gamespacepro.feature.dashboard

import com.gamespacepro.domain.accessibility.AccessibilityCapability
import com.gamespacepro.domain.accessibility.AccessibilityCapabilityDetector
import com.gamespacepro.domain.shell.ShellCapability
import com.gamespacepro.domain.shell.ShellCapabilityDetector
import com.gamespacepro.domain.shell.ShellPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeShellCapabilityDetector(
        var capability: ShellCapability,
    ) : ShellCapabilityDetector {
        override fun currentCapability(): ShellCapability = capability
    }

    private class FakeShellPermissionManager(
        private val grantResult: Boolean,
    ) : ShellPermissionManager {
        var requestCount = 0
            private set

        override suspend fun requestPermission(): Boolean {
            requestCount++
            return grantResult
        }
    }

    private class FakeAccessibilityCapabilityDetector(
        var capability: AccessibilityCapability,
    ) : AccessibilityCapabilityDetector {
        override fun currentCapability(): AccessibilityCapability = capability
    }

    @Test
    fun `initial state reflects both detectors at construction time`() {
        val viewModel = DashboardViewModel(
            shellCapabilityDetector = FakeShellCapabilityDetector(ShellCapability.READY),
            shellPermissionManager = FakeShellPermissionManager(grantResult = true),
            accessibilityCapabilityDetector = FakeAccessibilityCapabilityDetector(AccessibilityCapability.ENABLED),
        )

        assertEquals(ShellCapability.READY, viewModel.uiState.value.shellCapability)
        assertEquals(AccessibilityCapability.ENABLED, viewModel.uiState.value.accessibilityCapability)
        assertFalse(viewModel.uiState.value.isRequestingShellPermission)
    }

    @Test
    fun `refreshCapabilities re-reads both detectors`() {
        val shellDetector = FakeShellCapabilityDetector(ShellCapability.UNAVAILABLE)
        val viewModel = DashboardViewModel(
            shellCapabilityDetector = shellDetector,
            shellPermissionManager = FakeShellPermissionManager(grantResult = true),
            accessibilityCapabilityDetector = FakeAccessibilityCapabilityDetector(AccessibilityCapability.DISABLED),
        )

        shellDetector.capability = ShellCapability.READY
        viewModel.refreshCapabilities()

        assertEquals(ShellCapability.READY, viewModel.uiState.value.shellCapability)
    }

    @Test
    fun `onRequestShellPermissionClick calls the permission manager once and refreshes afterward`() = runTest {
        val shellDetector = FakeShellCapabilityDetector(ShellCapability.PERMISSION_REQUIRED)
        val permissionManager = FakeShellPermissionManager(grantResult = true)
        val viewModel = DashboardViewModel(
            shellCapabilityDetector = shellDetector,
            shellPermissionManager = permissionManager,
            accessibilityCapabilityDetector = FakeAccessibilityCapabilityDetector(AccessibilityCapability.DISABLED),
        )

        // Simulate the permission actually taking effect by the time
        // requestPermission() returns, same as the real Shizuku flow.
        shellDetector.capability = ShellCapability.READY

        viewModel.onRequestShellPermissionClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, permissionManager.requestCount)
        assertEquals(ShellCapability.READY, viewModel.uiState.value.shellCapability)
        assertFalse(viewModel.uiState.value.isRequestingShellPermission)
    }

    @Test
    fun `a second click while a request is already in flight is a no-op`() = runTest {
        val permissionManager = FakeShellPermissionManager(grantResult = true)
        val viewModel = DashboardViewModel(
            shellCapabilityDetector = FakeShellCapabilityDetector(ShellCapability.PERMISSION_REQUIRED),
            shellPermissionManager = permissionManager,
            accessibilityCapabilityDetector = FakeAccessibilityCapabilityDetector(AccessibilityCapability.DISABLED),
        )

        // Both calls happen before the dispatcher advances at all — this is
        // exactly the scenario that would double-fire requestPermission()
        // if the in-flight flag were set inside the launched coroutine
        // instead of synchronously before launching it.
        viewModel.onRequestShellPermissionClick()
        viewModel.onRequestShellPermissionClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, permissionManager.requestCount)
    }
}
