package com.gamespace.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gamespace.profile.ProfileType

/**
 * Nền gradient toàn app (navy sâu + 2 quầng sáng neon rất mờ, lấy cảm hứng từ vệt sáng
 * trên logo) — bọc quanh NavHost ở [com.gamespace.ui.MainActivity] để mọi màn hình đều
 * có cùng một nền thương hiệu thay vì `Surface` phẳng mặc định.
 */
@Composable
fun GsAppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GsBackgroundGradient),
    ) {
        // Quầng sáng xanh neon — góc trên phải.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(colors = listOf(GsPrimary.copy(alpha = 0.16f), Color.Transparent)),
                    CircleShape,
                )
                .blur(40.dp),
        )
        // Quầng sáng tím — góc dưới trái.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 60.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(colors = listOf(GsSecondary.copy(alpha = 0.14f), Color.Transparent)),
                    CircleShape,
                )
                .blur(40.dp),
        )
        content()
    }
}

/** Icon trong 1 khối tròn phát sáng nhẹ màu primary — dùng cho tiêu đề section (thay icon trơn). */
@Composable
fun GsIconBadge(icon: ImageVector, tint: Color = GsPrimary, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .background(tint.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Màu thương hiệu cho từng Performance Profile — dùng xuyên suốt UI (segmented button, HUD, badge...). */
fun ProfileType.brandColor(): Color = when (this) {
    ProfileType.LOW -> GsGreenLow
    ProfileType.BALANCED -> GsBlueBalanced
    ProfileType.PERFORMANCE -> GsRedPerformance
}

/** Gradient tương ứng cho từng Profile — dùng cho các nút CTA lớn theo Profile đang chọn. */
fun ProfileType.brandGradient(): Brush = when (this) {
    ProfileType.LOW -> Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981)))
    ProfileType.BALANCED -> GsAccentGradient
    ProfileType.PERFORMANCE -> Brush.linearGradient(listOf(Color(0xFFFF5470), Color(0xFFFF2E63)))
}

/** TopAppBar trong suốt để lộ nền gradient của [GsAppBackground] thay vì phủ 1 khối đặc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gsTransparentTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
)
