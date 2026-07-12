package com.gamespace.ui.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.diagnostics.DiagnosticsReport
import com.gamespace.ui.theme.gsTransparentTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.shareUri) {
        val uri = state.shareUri ?: return@LaunchedEffect
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ báo cáo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        viewModel.consumeShareEvent()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Diagnostics") }, colors = gsTransparentTopBarColors()) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = viewModel::exportDiagnostics, modifier = Modifier.weight(1f)) {
                    Text("Xuất báo cáo")
                }
                OutlinedButton(onClick = viewModel::exportLogs, modifier = Modifier.weight(1f)) {
                    Text("Xuất Log")
                }
            }

            if (state.isLoading && state.report == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Đang thu thập thông tin...")
                }
            } else {
                state.report?.let { report -> DiagnosticsContent(report) }
            }
        }
    }
}

@Composable
private fun DiagnosticsContent(report: DiagnosticsReport) {
    val info = report.deviceInfo
    LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DiagnosticsSection(
                title = "Thiết bị",
                rows = listOf(
                    "Model" to "${info.manufacturer} ${info.model}",
                    "Android" to "${info.androidVersion} (API ${info.apiLevel})",
                    "CPU" to "${info.cpuCoreCount} nhân · ${info.supportedAbis.firstOrNull() ?: "?"}",
                    "Màn hình" to "${info.screenWidthPx}x${info.screenHeightPx} @ ${info.currentRefreshRate}Hz",
                    "Game Mode API" to if (info.supportsGameMode) "Có" else "Không",
                    "Thermal API" to if (info.supportsThermalApi) "Có" else "Không",
                ),
            )
        }
        item {
            DiagnosticsSection(
                title = "Profile & Automation",
                rows = listOf(
                    "Profile hiện tại" to report.activeProfile.name,
                    "Gợi ý theo phần cứng" to info.suggestedProfile().name,
                    "Automation (lần gần nhất)" to if (report.isAutomationEnabled) "Đã bật" else "Đã tắt",
                ),
            )
        }
        item {
            DiagnosticsSection(
                title = "Quyền",
                rows = listOf(
                    "Shizuku cài đặt" to boolLabel(info.shizukuInstalled),
                    "Shizuku cấp quyền" to boolLabel(info.shizukuGranted),
                    "Overlay" to boolLabel(report.hasOverlayPermission),
                    "Usage Access" to boolLabel(report.hasUsageAccessPermission),
                    "Accessibility" to boolLabel(report.hasAccessibilityPermission),
                    "Notification Policy (DND)" to boolLabel(report.hasNotificationPolicyAccess),
                    "Post Notification" to boolLabel(report.hasPostNotificationPermission),
                ),
            )
        }
    }
}

private fun boolLabel(value: Boolean) = if (value) "✅ Có" else "❌ Không"

@Composable
private fun DiagnosticsSection(title: String, rows: List<Pair<String, String>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
