package com.gamespace.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
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

private fun TextUnit.scaledBy(scale: Float): TextUnit = when {
    !isSpecified -> this
    isSp -> (value * scale).sp
    isEm -> (value * scale).em
    else -> this
}

private fun TextStyle.scaledBy(scale: Float): TextStyle =
    copy(fontSize = fontSize.scaledBy(scale), lineHeight = lineHeight.scaledBy(scale))

/**
 * Toàn bộ cỡ chữ co giãn theo [scale] (từ [rememberGsScale]) thay vì cố định — cùng cơ chế
 * responsive với [GsSpacing], áp dụng 1 lần ở gốc [GameSpaceTheme] lên [GameSpaceTypography].
 */
fun Typography.scaledBy(scale: Float): Typography = copy(
    displayLarge = displayLarge.scaledBy(scale),
    displayMedium = displayMedium.scaledBy(scale),
    displaySmall = displaySmall.scaledBy(scale),
    headlineLarge = headlineLarge.scaledBy(scale),
    headlineMedium = headlineMedium.scaledBy(scale),
    headlineSmall = headlineSmall.scaledBy(scale),
    titleLarge = titleLarge.scaledBy(scale),
    titleMedium = titleMedium.scaledBy(scale),
    titleSmall = titleSmall.scaledBy(scale),
    bodyLarge = bodyLarge.scaledBy(scale),
    bodyMedium = bodyMedium.scaledBy(scale),
    bodySmall = bodySmall.scaledBy(scale),
    labelLarge = labelLarge.scaledBy(scale),
    labelMedium = labelMedium.scaledBy(scale),
    labelSmall = labelSmall.scaledBy(scale),
)
