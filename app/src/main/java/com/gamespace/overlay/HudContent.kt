package com.gamespace.overlay

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

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
    // TODO (tinh chỉnh UX): bubble thu gọn vừa clickable (mở rộng) vừa draggable (kéo thả),
    // 2 gesture detector có thể tranh chấp nhau khi ngón tay di chuyển rất ít lúc tap.
    // Cân nhắc dùng threshold khoảng cách hoặc `detectTapGestures` kết hợp `detectDragGestures`
    // trong cùng một `pointerInput` để phân biệt rõ tap và drag khi polish UI.
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            onDrag(dragAmount.x, dragAmount.y)
        }
    }

    if (!isExpanded) {
        CollapsedBubble(metrics = metrics, modifier = dragModifier, onClick = onToggleExpand)
    } else {
        ExpandedPanel(
            metrics = metrics,
            isDndActive = isDndActive,
            timerRemainingSeconds = timerRemainingSeconds,
            dragModifier = dragModifier,
            onCollapse = onToggleExpand,
            onDeepClean = onDeepClean,
            onToggleDnd = onToggleDnd,
            onCycleProfile = onCycleProfile,
            onToggleTimer = onToggleTimer,
        )
    }
}

@Composable
private fun CollapsedBubble(metrics: HudMetrics, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color(0xE6101826),
        contentColor = Color.White,
        modifier = modifier.size(56.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${metrics.fps}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text("FPS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
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
    onCollapse: () -> Unit,
    onDeepClean: () -> Unit,
    onToggleDnd: () -> Unit,
    onCycleProfile: () -> Unit,
    onToggleTimer: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xE6101826),
        contentColor = Color.White,
        modifier = Modifier.width(220.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(modifier = dragModifier, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${metrics.activeProfile.emoji} ${metrics.activeProfile.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                )
                IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.UnfoldLess, contentDescription = "Thu gọn", tint = Color.White)
                }
            }
            Spacer(Modifier.height(6.dp))

            MetricRow("FPS", "${metrics.fps}")
            MetricRow("RAM", "${metrics.ramAvailableMb}MB trống (${metrics.ramUsedPercent}% dùng)")
            MetricRow("Pin", "${metrics.batteryPercent}%${if (metrics.isCharging) " ⚡" else ""}")
            MetricRow("Nhiệt", metrics.thermalLabel)
            metrics.cpuUsagePercent?.let { MetricRow("CPU", "$it%") }
            timerRemainingSeconds?.let { seconds ->
                MetricRow("Timer", "%d:%02d".format(seconds / 60, seconds % 60))
            }

            Spacer(Modifier.height(8.dp))
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
private fun QuickToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(6.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
