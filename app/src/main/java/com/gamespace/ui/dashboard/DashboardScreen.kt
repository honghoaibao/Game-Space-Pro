package com.gamespace.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.hardware.DeviceInfo
import com.gamespace.optimizer.OptimizeResult
import com.gamespace.overlay.FloatingWebViewService
import com.gamespace.profile.ProfileApplyResult
import com.gamespace.profile.ProfileType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenLibrary: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenProtectedApps: () -> Unit = {},
    onOpenPerformanceCenter: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
        viewModel.refreshDeviceInfo() // bắt lại trạng thái Shizuku sau khi cấp quyền rồi quay lại app
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Space") },
                actions = {
                    OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Filled.Games, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Thư viện Game")
                    }
                    IconButton(onClick = onOpenDiagnostics, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Filled.Assessment, contentDescription = "Diagnostics")
                    }
                    IconButton(onClick = onOpenPerformanceCenter, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Filled.Speed, contentDescription = "Performance Center")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding),
        ) {
            item { ProfileSelectorCard(state, onSelect = viewModel::selectProfile) }
            item {
                AutomationCard(
                    state = state,
                    onToggleAutomation = viewModel::toggleAutomation,
                    overlayIntent = viewModel.overlayPermissionIntent(),
                    usageAccessIntent = viewModel.usageAccessIntent(),
                    accessibilityIntent = viewModel.accessibilitySettingsIntent(),
                    batteryOptimizationIntent = viewModel.ignoreBatteryOptimizationsIntent(),
                    oemAutoStartIntent = viewModel.oemAutoStartIntent(),
                )
            }
            item {
                DeviceInfoCard(
                    deviceInfo = state.deviceInfo,
                    onRequestShizukuPermission = viewModel::requestShizukuPermission,
                )
            }
            item {
                OptimizerCard(
                    state = state,
                    onDeepRamClean = viewModel::deepRamClean,
                    onClearCache = viewModel::clearCache,
                    onStorageTrim = viewModel::storageTrim,
                    onOpenProtectedApps = onOpenProtectedApps,
                )
            }
            item { PopupAppsCard(hasOverlayPermission = state.hasOverlayPermission) }
            item { FloatingChatCard() }
            state.lastApplyResult?.let { result ->
                item { ApplyResultCard(result) }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

private enum class StatusLevel { OK, WARNING, NEUTRAL }

/** Badge trạng thái dùng chung (Shizuku, permission...) — thay cho emoji chèn thẳng trong Text. */
@Composable
private fun StatusBadge(text: String, level: StatusLevel) {
    val (icon, color) = when (level) {
        StatusLevel.OK -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.tertiary
        StatusLevel.WARNING -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
        StatusLevel.NEUTRAL -> Icons.Filled.HelpOutline to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun AutomationCard(
    state: DashboardUiState,
    onToggleAutomation: () -> Unit,
    overlayIntent: Intent,
    usageAccessIntent: Intent,
    accessibilityIntent: Intent,
    batteryOptimizationIntent: Intent,
    oemAutoStartIntent: Intent?,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.PlayCircle, "Smart Automation")
            Spacer(Modifier.height(4.dp))
            Text(
                "Tự động phát hiện game, dọn nền, áp Profile và mở Overlay HUD khi vào game.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))

            if (!state.hasUsageAccessPermission) {
                PermissionRow(
                    label = "Cần quyền Usage Access để phát hiện game",
                    buttonLabel = "Cấp quyền",
                    onClick = { context.startActivity(usageAccessIntent) },
                )
            }
            if (!state.hasOverlayPermission) {
                PermissionRow(
                    label = "Cần quyền Overlay để hiển thị Floating HUD",
                    buttonLabel = "Cấp quyền",
                    onClick = { context.startActivity(overlayIntent) },
                )
            }
            if (!state.hasAccessibilityPermission) {
                PermissionRow(
                    label = "Tùy chọn: bật Accessibility để phát hiện game nhanh hơn",
                    buttonLabel = "Bật",
                    onClick = { context.startActivity(accessibilityIntent) },
                )
            }
            if (!state.hasBatteryOptimizationExemption) {
                PermissionRow(
                    label = "Miễn tối ưu pin cho GAME SPACE — tránh bị hệ thống tự tắt " +
                        "Overlay HUD/Popup Apps khi chạy nền",
                    buttonLabel = "Cấp quyền",
                    onClick = { runCatching { context.startActivity(batteryOptimizationIntent) } },
                )
            }
            if (oemAutoStartIntent != null) {
                PermissionRow(
                    label = "Máy ${Build.MANUFACTURER}: bật \"Tự khởi động\" cho GAME SPACE để " +
                        "khỏi bị dọn nền (cài đặt riêng của hãng, best-effort)",
                    buttonLabel = "Mở",
                    onClick = { runCatching { context.startActivity(oemAutoStartIntent) } },
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onToggleAutomation,
                enabled = state.hasUsageAccessPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (state.isAutomationRunning) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.isAutomationRunning) "Đang bật — Nhấn để tắt" else "Bật tự động hóa")
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, buttonLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onClick) { Text(buttonLabel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSelectorCard(
    state: DashboardUiState,
    onSelect: (ProfileType) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.Speed, "Performance Profile")
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ProfileType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.activeProfile == type,
                        onClick = { onSelect(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, ProfileType.entries.size),
                    ) {
                        Text("${type.emoji} ${type.displayName}")
                    }
                }
            }

            if (state.isApplyingProfile) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    deviceInfo: DeviceInfo?,
    onRequestShizukuPermission: () -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.PhoneAndroid, "Thiết bị")
            Spacer(Modifier.height(8.dp))

            if (deviceInfo == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đang phát hiện phần cứng...")
                }
                return@Column
            }

            InfoRow("Model", "${deviceInfo.manufacturer} ${deviceInfo.model}")
            InfoRow("Android", "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
            InfoRow("CPU", "${deviceInfo.cpuCoreCount} nhân · ${deviceInfo.supportedAbis.firstOrNull() ?: "?"}")
            InfoRow(
                "RAM",
                "${(deviceInfo.availableRamGb * 10).roundToInt() / 10.0} / " +
                    "${(deviceInfo.totalRamGb * 10).roundToInt() / 10.0} GB khả dụng",
            )
            InfoRow(
                "Màn hình",
                "${deviceInfo.screenWidthPx}×${deviceInfo.screenHeightPx} · " +
                    "${deviceInfo.currentRefreshRate.roundToInt()}Hz " +
                    "(hỗ trợ: ${deviceInfo.supportedRefreshRates.joinToString { "${it.roundToInt()}" }}Hz)",
            )
            InfoRow("Game Mode API", if (deviceInfo.supportsGameMode) "Có" else "Không")
            InfoRow("Thermal API", if (deviceInfo.supportsThermalApi) "Có" else "Không")

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Shizuku", style = MaterialTheme.typography.bodyMedium)
                when {
                    deviceInfo.shizukuGranted -> StatusBadge("Đã cấp quyền", StatusLevel.OK)
                    deviceInfo.shizukuInstalled -> StatusBadge("Đã cài, chưa cấp quyền", StatusLevel.WARNING)
                    else -> StatusBadge("Chưa cài", StatusLevel.NEUTRAL)
                }
            }
            if (deviceInfo.shizukuInstalled && !deviceInfo.shizukuGranted) {
                PermissionRow(
                    label = "Mở dialog Shizuku để cấp quyền cho GAME SPACE",
                    buttonLabel = "Cấp quyền",
                    onClick = onRequestShizukuPermission,
                )
            } else if (!deviceInfo.shizukuInstalled) {
                PermissionRow(
                    label = "Cài Shizuku (tùy chọn) để dùng Storage Trim, đổi refresh rate hệ thống...",
                    buttonLabel = "Cài đặt",
                    onClick = { openAppOrStore(context, "moe.shizuku.privileged.api") },
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Gợi ý Profile: ${deviceInfo.suggestedProfile().emoji} ${deviceInfo.suggestedProfile().displayName}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun OptimizerCard(
    state: DashboardUiState,
    onDeepRamClean: () -> Unit,
    onClearCache: () -> Unit,
    onStorageTrim: () -> Unit,
    onOpenProtectedApps: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.CleaningServices, "Optimizer Engine")
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDeepRamClean, enabled = !state.isOptimizing, modifier = Modifier.weight(1f)) {
                    Text("Dọn RAM")
                }
                Button(onClick = onClearCache, enabled = !state.isOptimizing, modifier = Modifier.weight(1f)) {
                    Text("Xóa Cache")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onStorageTrim,
                enabled = !state.isOptimizing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Storage Trim (cần Shizuku)")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenProtectedApps, modifier = Modifier.fillMaxWidth()) {
                Text("🎵 Quản lý app nhạc được bảo vệ khỏi dọn RAM")
            }

            if (state.isOptimizing) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.lastOptimizeResult?.let { result -> OptimizeResultSummary(result) }
        }
    }
}

@Composable
private fun OptimizeResultSummary(result: OptimizeResult) {
    Spacer(Modifier.height(12.dp))
    if (result.backgroundAppsKilled > 0 || result.ramFreedBytes > 0) {
        Text("✅ Đã dọn ${result.backgroundAppsKilled} tiến trình nền, giải phóng ~${result.ramFreedMb.roundToInt()}MB RAM")
    }
    if (result.protectedAppsSkipped > 0) {
        Text("🎵 Đã bảo vệ ${result.protectedAppsSkipped} app nhạc khỏi bị dọn")
    }
    if (result.cacheClearedBytes > 0) {
        Text("✅ Đã xóa ~${result.cacheClearedMb.roundToInt()}MB cache của Game Space")
    }
    if (result.fstrimRan) {
        Text("✅ Đã chạy Storage Trim")
    }
    result.skippedActions.forEach { Text("⏭️ $it", style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun PopupAppsCard(hasOverlayPermission: Boolean) {
    val context = LocalContext.current
    var customUrl by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.OpenInNew, "Popup Apps")
            Spacer(Modifier.height(4.dp))
            Text(
                "Mở trình duyệt nổi để tra cứu mà không cần rời game. Không thể ép TikTok/Chrome/" +
                    "Zalo thật chạy nổi — đó là giới hạn của Android, không phải thiếu sót (xem ADR-001).",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))

            if (!hasOverlayPermission) {
                Text(
                    "Cần cấp quyền Overlay ở mục Smart Automation bên trên trước khi dùng.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickWebButton("ChatGPT", hasOverlayPermission) {
                    FloatingWebViewService.start(context, "https://chatgpt.com")
                }
                QuickWebButton("Gemini", hasOverlayPermission) {
                    FloatingWebViewService.start(context, "https://gemini.google.com")
                }
                QuickWebButton("Wiki", hasOverlayPermission) {
                    FloatingWebViewService.start(context, "https://vi.wikipedia.org")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("URL tùy chỉnh") },
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { FloatingWebViewService.start(context, customUrl.ifBlank { null }) },
                    enabled = hasOverlayPermission,
                ) { Text("Mở") }
            }

            Spacer(Modifier.height(12.dp))
            Text("Picture-in-Picture", style = MaterialTheme.typography.labelLarge)
            Text(
                "GAME SPACE không thể tự bật PiP cho app khác — chỉ mở được app hỗ trợ (vd. YouTube), " +
                    "bạn tự bật PiP từ trong app đó.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mở YouTube") }
        }
    }
}

@Composable
private fun RowScope.QuickWebButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
        Text(label)
    }
}

private data class ChatApp(val label: String, val packageName: String)

private val CHAT_APPS = listOf(
    ChatApp("Discord", "com.discord"),
    ChatApp("Telegram", "org.telegram.messenger"),
    ChatApp("Messenger", "com.facebook.orca"),
    ChatApp("Zalo", "com.zing.zalo"),
)

@Composable
private fun FloatingChatCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.Chat, "Floating Chat")
            Spacer(Modifier.height(4.dp))
            Text(
                "Mở nhanh app chat mà không cần rời game — chỉ là shortcut mở app thật, " +
                    "KHÔNG hiển thị nội dung Discord/Telegram/Messenger/Zalo nổi trên màn hình " +
                    "(Android không cho app khác làm điều đó, xem ADR-001).",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CHAT_APPS.take(2).forEach { app ->
                    QuickWebButton(app.label, true) { openAppOrStore(context, app.packageName) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CHAT_APPS.drop(2).forEach { app ->
                    QuickWebButton(app.label, true) { openAppOrStore(context, app.packageName) }
                }
            }
        }
    }
}

/** Mở app nếu đã cài; nếu chưa (hoặc package name không khớp máy này), mở trang Play Store của app đó. */
private fun openAppOrStore(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return
    }
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(marketIntent) }.onFailure {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(webIntent)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ApplyResultCard(result: ProfileApplyResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.Memory, "Đã áp dụng: ${result.profileType.displayName}")
            Spacer(Modifier.height(8.dp))

            result.appliedActions.forEach { Text("✅ $it") }
            result.skippedActions.forEach { Text("⏭️ $it") }

            if (result.pendingSystemIntents.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val context = LocalContext.current
                result.pendingSystemIntents.forEach { (label, intent) ->
                    Button(onClick = { context.startActivity(intent) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
