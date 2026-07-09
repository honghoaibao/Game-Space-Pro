package com.gamespacepro.feature.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespacepro.domain.accessibility.AccessibilityCapability
import com.gamespacepro.domain.shell.ShellCapability
import com.gamespacepro.ui.theme.GameSpaceProTheme

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DashboardScreen(
        modifier = modifier,
        uiState = uiState,
        onRequestShellPermissionClick = viewModel::onRequestShellPermissionClick,
        onRefreshClick = viewModel::refreshCapabilities,
        onOpenAccessibilitySettingsClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRequestShellPermissionClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenAccessibilitySettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CapabilityCard(
                title = stringResource(R.string.dashboard_shizuku_card_title),
                statusText = uiState.shellCapability.toDisplayText(),
                actionLabel = uiState.shellCapability.toActionLabelOrNull(),
                isActionInProgress = uiState.isRequestingShellPermission,
                onActionClick = onRequestShellPermissionClick,
            )
            CapabilityCard(
                title = stringResource(R.string.dashboard_accessibility_card_title),
                statusText = uiState.accessibilityCapability.toDisplayText(),
                actionLabel = uiState.accessibilityCapability.toActionLabelOrNull(),
                isActionInProgress = false,
                onActionClick = onOpenAccessibilitySettingsClick,
            )
            Button(onClick = onRefreshClick) {
                Text(stringResource(R.string.dashboard_refresh_action))
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    title: String,
    statusText: String,
    actionLabel: String?,
    isActionInProgress: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onActionClick, enabled = !isActionInProgress) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                    } else {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellCapability.toDisplayText(): String = when (this) {
    ShellCapability.UNAVAILABLE -> stringResource(R.string.shell_capability_unavailable)
    ShellCapability.PERMISSION_REQUIRED -> stringResource(R.string.shell_capability_permission_required)
    ShellCapability.PERMISSION_DENIED -> stringResource(R.string.shell_capability_permission_denied)
    ShellCapability.READY -> stringResource(R.string.shell_capability_ready)
}

/** `null` when there's nothing an in-app button can do about the current state. */
@Composable
private fun ShellCapability.toActionLabelOrNull(): String? = when (this) {
    ShellCapability.UNAVAILABLE -> null // Shizuku itself must be installed/paired outside this app
    ShellCapability.PERMISSION_REQUIRED,
    ShellCapability.PERMISSION_DENIED,
    -> stringResource(R.string.dashboard_request_permission_action)
    ShellCapability.READY -> null
}

@Composable
private fun AccessibilityCapability.toDisplayText(): String = when (this) {
    AccessibilityCapability.DISABLED -> stringResource(R.string.accessibility_capability_disabled)
    AccessibilityCapability.ENABLED -> stringResource(R.string.accessibility_capability_enabled)
}

@Composable
private fun AccessibilityCapability.toActionLabelOrNull(): String? = when (this) {
    AccessibilityCapability.DISABLED -> stringResource(R.string.dashboard_open_settings_action)
    AccessibilityCapability.ENABLED -> null
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    GameSpaceProTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                shellCapability = ShellCapability.PERMISSION_REQUIRED,
                accessibilityCapability = AccessibilityCapability.DISABLED,
            ),
            onRequestShellPermissionClick = {},
            onRefreshClick = {},
            onOpenAccessibilitySettingsClick = {},
        )
    }
}
