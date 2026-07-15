package com.gamespace.overlay

import android.os.PowerManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gamespace.ui.icons.filled.Bolt
import com.gamespace.ui.icons.filled.CleaningServices
import com.gamespace.ui.icons.filled.Memory
import com.gamespace.ui.icons.filled.NotificationsOff
import com.gamespace.ui.icons.filled.Speed
import com.gamespace.ui.icons.filled.SwapHoriz
import com.gamespace.ui.icons.filled.Timer
import com.gamespace.ui.icons.filled.UnfoldLess
import com.gamespace.ui.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamespace.ui.theme.GsError
import com.gamespace.ui.theme.GsOnSurface
import com.gamespace.ui.theme.GsOnSurfaceMuted
import com.gamespace.ui.theme.GsStatNumberStyle
import com.gamespace.ui.theme.GsSurfaceElevated
import com.gamespace.ui.theme.GsWarning
import com.gamespace.ui.theme.brandColor
import kotlin.math.abs

/**
 * Redesign sâu (so với bản trước chỉ có list text phẳng): bubble thu gọn có badge cảnh báo
 * nhanh (không cần mở rộng vẫn thấy máy đang nóng/pin yếu), panel mở rộng chia thành lưới
 * "stat tile" (icon + số lớn + nhãn) thay vì các dòng text đơn điệu, chip trạng thái tô màu
 * theo mức độ (Nhiệt/Pin), chuyển cảnh thu gọn↔mở rộng bằng Crossfade + animateContentSize thay
 * vì cắt cứng. Vẫn giữ hiệu ứng NHẸ (không dùng blur — tốn GPU khi vẽ đè lên game đang chạy).
 */
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

    // animateContentSize: khung ngoài co giãn mượt giữa kích thước hình tròn (58dp) và panel
    // (232dp) thay vì cắt cứng. Crossfade: nội dung bên trong mờ dần qua lại thay vì bật/tắt
    // đột ngột. Cả 2 đều là animation NGẮN, có điểm dừng (không lặp vô hạn) nên chi phí GPU/CPU
    // không đáng kể dù đang vẽ đè lên 1 game khác.
    Box(modifier = Modifier.animateContentSize(animationSpec = tween(180))) {
        Crossfade(targetState = isExpanded, animationSpec = tween(150), label = "hud-expand") { expanded ->
            if (!expanded) {
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
    }
}

@Composable
private fun CollapsedBubble(metrics: HudMetrics, modifier: Modifier) {
    val profileColor = metrics.activeProfile.brandColor()
    val hasWarning = metrics.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE ||
        (metrics.batteryPercent <= 15 && !metrics.isCharging)

    Box {
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
                    Text("${metrics.fps}", style = GsStatNumberStyle.copy(fontSize = 18.sp), color = GsOnSurface)
                    Text("FPS", style = MaterialTheme.typography.labelSmall, color = GsOnSurfaceMuted)
                }
            }
        }
        // Badge cảnh báo nhanh — thấy ngay máy đang nóng/pin yếu mà KHÔNG cần mở rộng panel.
        if (hasWarning) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .border(1.5.dp, GsSurfaceElevated, CircleShape)
                    .background(GsError, CircleShape),
            )
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
    val ramColor = percentColor(metrics.ramUsedPercent)
    val cpuColor = percentColor(metrics.cpuUsagePercent)
    val thermalColor = thermalColor(metrics.thermalStatus)
    val dividerColor = GsOnSurfaceMuted.copy(alpha = 0.12f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GsSurfaceElevated.copy(alpha = 0.97f),
        contentColor = GsOnSurface,
        modifier = Modifier
            .width(232.dp)
            .border(1.dp, Brush.linearGradient(listOf(profileColor.copy(alpha = 0.7f), Color.Transparent)), RoundedCornerShape(20.dp)),
    ) {
        Column(Modifier.padding(12.dp)) {
            // Header — chấm màu Profile + tên + icon thu gọn. Cả Row nằm trong dragModifier
            // nên kéo/chạm ở phần header cũng điều khiển được popup (không cần chạm đúng icon).
            Row(modifier = dragModifier, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(profileColor, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    metrics.activeProfile.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    color = GsOnSurface,
                )
                Icon(Icons.Filled.UnfoldLess, contentDescription = "Thu gọn", tint = GsOnSurfaceMuted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(10.dp))

            // Lưới 3 stat chính — icon + số lớn + nhãn, tô màu theo ngưỡng thay vì chỉ có text.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatTile(icon = null, value = "${metrics.fps}", label = "FPS", valueColor = GsOnSurface)
                StatTile(icon = Icons.Filled.Memory, value = "${metrics.ramUsedPercent}%", label = "RAM", valueColor = ramColor)
                StatTile(
                    icon = Icons.Filled.Speed,
                    value = metrics.cpuUsagePercent?.let { "$it%" } ?: "—",
                    label = "CPU",
                    valueColor = cpuColor,
                )
            }
            Spacer(Modifier.height(10.dp))

            // Chip trạng thái Nhiệt/Pin — nền màu nhạt theo mức độ, chỉ hiện icon cảnh báo khi
            // thật sự cần chú ý (giữ giao diện gọn khi mọi thứ bình thường).
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip(
                    label = "Nhiệt",
                    value = metrics.thermalLabel,
                    color = thermalColor,
                    icon = if (metrics.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE) Icons.Filled.WarningAmber else null,
                )
                StatusChip(
                    label = "Pin",
                    value = "${metrics.batteryPercent}%",
                    color = if (metrics.isCharging) GsWarning else GsOnSurfaceMuted,
                    icon = if (metrics.isCharging) Icons.Filled.Bolt else null,
                )
            }
            timerRemainingSeconds?.let { seconds ->
                Spacer(Modifier.height(6.dp))
                StatusChip(
                    label = "Timer",
                    value = "%d:%02d".format(seconds / 60, seconds % 60),
                    color = profileColor,
                    icon = Icons.Filled.Timer,
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = dividerColor)
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
private fun StatTile(icon: ImageVector?, value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.height(2.dp))
        }
        Text(value, style = GsStatNumberStyle.copy(fontSize = 16.sp), color = valueColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = GsOnSurfaceMuted)
    }
}

@Composable
private fun StatusChip(label: String, value: String, color: Color, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = GsOnSurfaceMuted)
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun QuickToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
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

/** Ngưỡng màu dùng chung cho RAM/CPU: >=85% báo động đỏ, >=65% cảnh báo vàng, còn lại bình thường. */
private fun percentColor(percent: Int?): Color = when {
    percent == null -> GsOnSurfaceMuted
    percent >= 85 -> GsError
    percent >= 65 -> GsWarning
    else -> GsOnSurface
}

private fun thermalColor(status: Int): Color = when {
    status >= PowerManager.THERMAL_STATUS_SEVERE -> GsError
    status >= PowerManager.THERMAL_STATUS_MODERATE -> GsWarning
    else -> GsOnSurfaceMuted
}

private val GsAccentGradientStart = Color.White.copy(alpha = 0.10f)
private val GsAccentGradientEnd = Color.White.copy(alpha = 0.04f)
