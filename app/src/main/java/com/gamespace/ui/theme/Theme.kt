package com.gamespace.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// GAME SPACE luôn dùng giao diện tối — đây là app "phòng game/booster", nền sáng phá vỡ
// cảm giác HUD/turbo và khiến overlay/Floating HUD (vốn thiết kế cho nền tối) bị lệch tông
// khi mở cùng lúc. Bỏ hẳn nhánh Light Theme thay vì để dở dang/không đồng bộ.
private val GameSpaceColorScheme = darkColorScheme(
    primary = GsPrimary,
    onPrimary = GsOnAccent,
    primaryContainer = GsPrimaryDeep,
    onPrimaryContainer = GsOnSurface,
    secondary = GsSecondary,
    onSecondary = GsOnAccent,
    tertiary = GsSuccess,
    onTertiary = GsOnAccent,
    background = GsBackgroundBottom,
    onBackground = GsOnSurface,
    surface = GsSurface,
    onSurface = GsOnSurface,
    surfaceVariant = GsSurfaceElevated,
    onSurfaceVariant = GsOnSurfaceMuted,
    outline = GsSurfaceHairline,
    error = GsError,
    onError = GsOnAccent,
)

/** Bo góc rộng hơn mặc định Material3 — cảm giác "app booster cao cấp" thay vì phẳng/hành chính. */
private val GameSpaceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun GameSpaceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GameSpaceColorScheme,
        typography = GameSpaceTypography,
        shapes = GameSpaceShapes,
        content = content,
    )
}
