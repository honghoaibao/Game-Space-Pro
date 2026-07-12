package com.gamespace.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gamespace.ui.theme.GsOnSurface
import com.gamespace.ui.theme.GsOnSurfaceMuted
import com.gamespace.ui.theme.GsSurfaceElevated
import com.gamespace.ui.theme.brandColor
import kotlin.math.abs

@Composable
fun HudContent(
    metrics: HudMetrics,
    isExpanded: Boolean,
    isDndActive: Boolean,
    timerRemainingSeconds: Int?,
    onToggleExpand: () -> Unit,
    onDeepClean: () -> Unit,
    onToggleDnd: () -> Unit,
    onCycleProfile: () -> Unit,
    onToggleTimer: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
) {
    // Trước đây bubble thu gọn dùng 2 gesture detector riêng (click + drag) tranh chấp
    // nhau, khiến những cú chạm nhẹ đôi khi bị nuốt thành "drag cực nhỏ" thay vì mở rộng
    // panel — cảm giác như "bấm vào popup không có phản hồi gì". Gộp lại 1 detector duy
    // nhất: nếu tổng khoảng cách kéo dưới ngưỡng touch-slop của hệ thống thì coi là tap.
    val dragOrTapModifier = Modifier.pointerInput(Unit) {
        var totalDrag = 0f
        detectDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragEnd = { if (totalDrag < viewConfiguration.touchSlop) onToggleExpand() },
        ) { change, dragAmount ->
            change.consume()
            totalDrag += abs(dragAmount.x) + abs(dragAmount.y)
            onDrag(dragAmount.x, dragAmount.y)
        }
    }

    if (!isExpanded) {
        CollapsedBubble(metrics = metrics, modifier = dragOrTapModifier)
    } else {
        ExpandedPanel(
            metrics = metrics,
            isDndActive = isDndActive,
            timerRemainingSeconds = timerRemainingSeconds,
            dragModifier = dragOrTapModifier,
            onDeepClean = onDeepClean,
            onToggleDnd = onToggleDnd,
            onCycleProfile = onCycleProfile,
            onToggleTimer = onToggleTimer,
        )
    }
}

@Composable
private fun CollapsedBubble(metrics: HudMetrics, modifier: Modifier) {
    val profileColor = metrics.activeProfile.brandColor()
    Surface(
        shape = CircleShape,
        color = GsSurfaceElevated.copy(alpha = 0.94f),
        contentColor = GsOnSurface,
        modifier = modifier
            .size(58.dp)
            .border(1.5.dp, Brush.linearGradient(listOf(profileColor, profileColor.copy(alpha = 0.25f))), CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${metrics.fps}", style = MaterialTheme.typography.titleSmall, color = GsOnSurface)
                Text("FPS", style = MaterialTheme.typography.labelSmall, color = GsOnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun ExpandedPanel(
    metrics: HudMetrics,
    isDndActive: Boolean,
    timerRemainingSeconds: Int?,
    dragModifier: Modifier,
    onDeepClean: () -> Unit,
    onToggleDnd: () -> Unit,
    onCycleProfile: () -> Unit,
    onToggleTimer: () -> Unit,
) {
    val profileColor = metrics.activeProfile.brandColor()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GsSurfaceElevated.copy(alpha = 0.96f),
        contentColor = GsOnSurface,
        modifier = Modifier
            .width(228.dp)
            .border(1.dp, Brush.linearGradient(listOf(profileColor.copy(alpha = 0.6f), Color.Transparent)), RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(modifier = dragModifier, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(profileColor, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${metrics.activeProfile.emoji} ${metrics.activeProfile.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    color = GsOnSurface,
                )
                // "onToggleExpand" giờ được xử lý qua chạm-để-thu-gọn trên chính icon này
                // (dùng cùng cơ chế tap/drag ở trên qua dragModifier bọc cả Row tiêu đề).
                Icon(Icons.Filled.UnfoldLess, contentDescription = "Thu gọn", tint = GsOnSurfaceMuted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))

            MetricRow("FPS", "${metrics.fps}")
            MetricRow("RAM", "${metrics.ramAvailableMb}MB trống (${metrics.ramUsedPercent}% dùng)")
            MetricRow("Pin", "${metrics.batteryPercent}%${if (metrics.isCharging) " ⚡" else ""}")
            MetricRow("Nhiệt", metrics.thermalLabel)
            MetricRow("CPU", metrics.cpuUsagePercent?.let { "$it%" } ?: "Đang đo…")
            timerRemainingSeconds?.let { seconds ->
                MetricRow("Timer", "%d:%02d".format(seconds / 60, seconds % 60))
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                QuickToolButton(Icons.Filled.CleaningServices, "Dọn RAM", onDeepClean)
                QuickToolButton(
                    Icons.Filled.NotificationsOff,
                    if (isDndActive) "Bỏ chặn" else "Chặn TB",
                    onToggleDnd,
                )
                QuickToolButton(Icons.Filled.Timer, "Timer", onToggleTimer)
                QuickToolButton(Icons.Filled.SwapHoriz, "Đổi Profile", onCycleProfile)
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = GsOnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = GsOnSurface)
    }
}

@Composable
private fun QuickToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Brush.verticalGradient(listOf(GsAccentGradientStart, GsAccentGradientEnd)), RoundedCornerShape(10.dp))
            .padding(6.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
            Icon(icon, contentDescription = label, tint = GsOnSurface, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = GsOnSurface)
    }
}

private val GsAccentGradientStart = Color.White.copy(alpha = 0.10f)
private val GsAccentGradientEnd = Color.White.copy(alpha = 0.04f)
