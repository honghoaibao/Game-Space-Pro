package com.gamespace.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bộ khoảng cách/kích thước responsive — thay cho việc khóa cứng `dp` rải rác khắp UI. Mọi
 * Card/Spacer/Icon trong bản redesign này lấy giá trị từ [LocalGsSpacing] thay vì hằng số cố
 * định, để tự thích ứng theo tỉ lệ màn hình/độ phân giải máy thật (điện thoại nhỏ, máy lớn,
 * tablet) thay vì 1 kích thước cứng cho mọi máy.
 */
data class GsSpacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val cardCorner: Dp,
    val iconSize: Dp,
    val iconSizeLarge: Dp,
)

// Baseline đo trên màn hình 360dp width (phổ biến nhất Android hiện nay).
private const val BASE_XS = 4f
private const val BASE_SM = 8f
private const val BASE_MD = 16f
private const val BASE_LG = 24f
private const val BASE_XL = 32f
private const val BASE_XXL = 48f
private const val BASE_CARD_CORNER = 20f
private const val BASE_ICON = 40f
private const val BASE_ICON_LARGE = 64f
private const val BASELINE_WIDTH_DP = 360f

/** Scale mặc định = 1f (dùng khi chưa nằm trong [GameSpaceTheme], ví dụ @Preview). */
val LocalGsSpacing = staticCompositionLocalOf {
    GsSpacing(
        xs = BASE_XS.dp, sm = BASE_SM.dp, md = BASE_MD.dp, lg = BASE_LG.dp,
        xl = BASE_XL.dp, xxl = BASE_XXL.dp, cardCorner = BASE_CARD_CORNER.dp,
        iconSize = BASE_ICON.dp, iconSizeLarge = BASE_ICON_LARGE.dp,
    )
}

/**
 * Hệ số scale UI dựa theo bề rộng màn hình thực tế (dp) so với baseline 360dp — clamp trong
 * [0.85, 1.3] để máy quá nhỏ (icon/chữ không bị bóp quá bé) hoặc quá to/tablet (không bị giãn
 * quá đà vỡ layout) vẫn giữ được tỉ lệ hợp lý.
 */
@Composable
fun rememberGsScale(): Float {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return (widthDp / BASELINE_WIDTH_DP).coerceIn(0.85f, 1.3f)
}

/** Tính [GsSpacing] thực tế cho phiên hiện tại theo [scale] — gọi 1 lần ở gốc [GameSpaceTheme]. */
fun gsSpacingFor(scale: Float): GsSpacing = GsSpacing(
    xs = (BASE_XS * scale).dp,
    sm = (BASE_SM * scale).dp,
    md = (BASE_MD * scale).dp,
    lg = (BASE_LG * scale).dp,
    xl = (BASE_XL * scale).dp,
    xxl = (BASE_XXL * scale).dp,
    cardCorner = (BASE_CARD_CORNER * scale).dp,
    iconSize = (BASE_ICON * scale).dp,
    iconSizeLarge = (BASE_ICON_LARGE * scale).dp,
)
