package com.gamespace.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tinh chỉnh cho cảm giác "turbo/booster" cao cấp — số liệu (FPS/RAM/%...)
 * đậm hơn mặc định Material3, tiêu đề có letter-spacing rộng nhẹ kiểu HUD game, không
 * dùng font ngoài (giữ font hệ thống để không tăng dung lượng APK).
 */
val GameSpaceTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.Bold,
        ),
        titleMedium = base.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp,
        ),
        titleSmall = base.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        labelLarge = base.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            letterSpacing = 0.1.sp,
        ),
    )
}

/** Kiểu số liệu lớn kiểu HUD (FPS/CPU/RAM...) — dùng trong StatChip/MetricRow nổi bật. */
val GsStatNumberStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 20.sp,
    letterSpacing = 0.sp,
)
