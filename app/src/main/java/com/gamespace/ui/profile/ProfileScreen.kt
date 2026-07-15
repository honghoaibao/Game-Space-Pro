package com.gamespace.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gamespace.ui.icons.filled.CheckCircle
import com.gamespace.ui.icons.filled.PhoneAndroid
import com.gamespace.ui.icons.filled.Speed
import com.gamespace.ui.icons.filled.Tune
import com.gamespace.ui.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.hardware.DeviceInfo
import com.gamespace.profile.ProfileType
import com.gamespace.ui.theme.GsIconBadge
import com.gamespace.ui.theme.GsOnSurface
import com.gamespace.ui.theme.GsOnSurfaceMuted
import com.gamespace.ui.theme.GsPrimary
import com.gamespace.ui.theme.GsSurfaceElevated
import com.gamespace.ui.theme.LocalGsSpacing
import com.gamespace.ui.theme.brandColor
import com.gamespace.ui.theme.brandIcon
import com.gamespace.ui.theme.gsTransparentTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileScreenViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalGsSpacing.current
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Hồ sơ") }, colors = gsTransparentTopBarColors()) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                SectionTitle(icon = Icons.Filled.Tune, title = "Chế độ hiệu năng")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ProfileType.entries.forEach { type ->
                        ProfileCard(
                            type = type,
                            isSelected = state.activeProfile == type,
                            deviceInfo = state.deviceInfo,
                            isApplying = state.isApplyingProfile,
                            onClick = { viewModel.selectProfile(type) },
                        )
                    }
                }
            }
            state.lastApplyResult?.let { result ->
                item { ApplyResultSummary(applied = result.appliedActions, skipped = result.skippedActions) }
            }

            item {
                SectionTitle(icon = Icons.Filled.Speed, title = "Theo dõi thời gian chơi")
            }
            item {
                TrackingCard(
                    isEnabled = state.isTrackingEnabled,
                    hasPermission = state.hasUsageAccessPermission,
                    onToggle = { viewModel.toggleTracking() },
                    onGrantPermission = { context.startActivity(viewModel.usageAccessIntent()) },
                )
            }

            item {
                SectionTitle(icon = Icons.Filled.PhoneAndroid, title = "Thiết bị của bạn")
            }
            item {
                DeviceInfoCard(deviceInfo = state.deviceInfo)
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    val spacing = LocalGsSpacing.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        GsIconBadge(icon)
        Spacer(Modifier.width(spacing.sm))
        Text(title, style = MaterialTheme.typography.titleMedium, color = GsOnSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileCard(
    type: ProfileType,
    isSelected: Boolean,
    deviceInfo: DeviceInfo?,
    isApplying: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalGsSpacing.current
    val color = type.brandColor()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(spacing.cardCorner),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.16f) else GsSurfaceElevated,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(spacing.iconSize).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(type.brandIcon(), contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(type.displayName, style = MaterialTheme.typography.titleSmall, color = GsOnSurface)
                Text(
                    profileDescription(type, deviceInfo),
                    style = MaterialTheme.typography.bodySmall,
                    color = GsOnSurfaceMuted,
                )
            }
            if (isSelected && isApplying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else if (isSelected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Đang dùng", tint = color)
            }
        }
    }
}

private fun profileDescription(type: ProfileType, deviceInfo: DeviceInfo?): String {
    val maxRate = deviceInfo?.supportedRefreshRates?.maxOrNull()?.toInt()
    return when (type) {
        ProfileType.LOW -> "Giới hạn 60Hz, giảm độ phân giải, dọn RAM trước khi vào game — tiết kiệm pin tối đa"
        ProfileType.BALANCED -> "Giữ nguyên cấu hình mặc định của máy — cân bằng hiệu năng và thời lượng pin"
        ProfileType.PERFORMANCE ->
            if (maxRate != null) "Tần số quét cao nhất máy hỗ trợ (${maxRate}Hz), ưu tiên hiệu năng tối đa"
            else "Tần số quét cao nhất máy hỗ trợ, ưu tiên hiệu năng tối đa"
    }
}

@Composable
private fun ApplyResultSummary(applied: List<String>, skipped: List<String>) {
    val spacing = LocalGsSpacing.current
    if (applied.isEmpty() && skipped.isEmpty()) return
    Card(
        shape = RoundedCornerShape(spacing.cardCorner),
        colors = CardDefaults.cardColors(containerColor = GsSurfaceElevated.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md)) {
            applied.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GsPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text(line, style = MaterialTheme.typography.bodySmall, color = GsOnSurfaceMuted)
                }
            }
            skipped.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = GsOnSurfaceMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text(line, style = MaterialTheme.typography.bodySmall, color = GsOnSurfaceMuted)
                }
            }
        }
    }
}

@Composable
private fun TrackingCard(
    isEnabled: Boolean,
    hasPermission: Boolean,
    onToggle: () -> Unit,
    onGrantPermission: () -> Unit,
) {
    val spacing = LocalGsSpacing.current
    Card(
        shape = RoundedCornerShape(spacing.cardCorner),
        colors = CardDefaults.cardColors(containerColor = GsSurfaceElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tự động tối ưu & theo dõi", style = MaterialTheme.typography.titleSmall, color = GsOnSurface)
                    Text(
                        "Ghi nhận thời gian chơi cho tab \"Thời gian chơi\" và tự áp Profile khi vào game",
                        style = MaterialTheme.typography.bodySmall,
                        color = GsOnSurfaceMuted,
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    enabled = hasPermission,
                    colors = SwitchDefaults.colors(checkedThumbColor = GsPrimary),
                )
            }
            if (!hasPermission) {
                Spacer(Modifier.width(spacing.sm))
                TextButton(onClick = onGrantPermission) {
                    Text("Cấp quyền truy cập lịch sử sử dụng để bật")
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(deviceInfo: DeviceInfo?) {
    val spacing = LocalGsSpacing.current
    Card(
        shape = RoundedCornerShape(spacing.cardCorner),
        colors = CardDefaults.cardColors(containerColor = GsSurfaceElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md)) {
            if (deviceInfo == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(spacing.sm))
                    Text("Đang phát hiện phần cứng...", style = MaterialTheme.typography.bodySmall, color = GsOnSurfaceMuted)
                }
            } else {
                DeviceInfoRow(label = "Máy", value = "${deviceInfo.manufacturer} ${deviceInfo.model}")
                DeviceInfoRow(label = "Android", value = "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
                DeviceInfoRow(label = "CPU", value = "${deviceInfo.cpuCoreCount} nhân")
                DeviceInfoRow(
                    label = "RAM",
                    value = "%.1fGB trống / %.1fGB".format(deviceInfo.availableRamGb, deviceInfo.totalRamGb),
                )
                DeviceInfoRow(label = "Độ phân giải", value = "${deviceInfo.screenWidthPx}×${deviceInfo.screenHeightPx}")
                if (deviceInfo.supportedRefreshRates.isNotEmpty()) {
                    DeviceInfoRow(
                        label = "Tần số quét",
                        value = deviceInfo.supportedRefreshRates.sorted().joinToString(" / ") { "${it.toInt()}Hz" },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    val spacing = LocalGsSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = GsOnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = GsOnSurface)
    }
}
