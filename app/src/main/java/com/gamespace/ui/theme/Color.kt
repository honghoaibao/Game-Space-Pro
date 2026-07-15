package com.gamespace.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Bảng màu GAME SPACE — lấy trực tiếp từ logo app (xanh neon + navy đậm) ─────────
// Đã đo màu thật từ logo_full.png: accent xanh neon ~#00B3FD, nền navy ~#062240.
// Mục tiêu: cảm giác "turbo/booster" cao cấp — nền tối sâu, chi tiết phát sáng neon,
// giống các app Game Turbo/GFX Tool thương mại thay vì Material mặc định phẳng.

// Accent chính — xanh neon của logo.
val GsPrimary = Color(0xFF00B3FD)
val GsPrimaryBright = Color(0xFF3FD4FF)
val GsPrimaryDeep = Color(0xFF0072B8)

// Accent phụ — tím điện để tạo gradient "turbo" 2 tông, tương phản với xanh neon.
val GsSecondary = Color(0xFF8B5CF6)
val GsSecondaryBright = Color(0xFFB794FF)

// Nền — navy gần đen, cùng tông với nền logo, nhiều lớp độ sâu cho elevation.
val GsBackgroundTop = Color(0xFF060C16)
val GsBackgroundBottom = Color(0xFF0B1830)
val GsSurface = Color(0xFF101B2E)
val GsSurfaceElevated = Color(0xFF16243B)
val GsSurfaceHairline = Color(0xFF24344D)
val GsOnSurface = Color(0xFFEAF2FB)
val GsOnSurfaceMuted = Color(0xFFA9B8CE)

// Trạng thái Profile — Low/Balanced/Performance, đủ tương phản trên nền tối.
val GsGreenLow = Color(0xFF34D399)
val GsBlueBalanced = GsPrimary
val GsRedPerformance = Color(0xFFFF5470)

// Semantic.
val GsSuccess = Color(0xFF34D399)
val GsWarning = Color(0xFFFFB020)
val GsError = Color(0xFFFF5470)

/** Chữ/icon đặt trên nền màu sáng (primary/secondary/error) — navy gần đen, tương phản tốt hơn trắng. */
val GsOnAccent = Color(0xFF04101F)

/** Gradient nền toàn app — navy sâu, dùng cho Scaffold/background lớp ngoài cùng. */
val GsBackgroundGradient = Brush.verticalGradient(
    colors = listOf(GsBackgroundTop, GsBackgroundBottom),
)

/** Gradient "turbo" 2 tông xanh-tím — dùng cho CTA chính, thanh tiêu đề, badge nổi bật. */
val GsAccentGradient = Brush.linearGradient(
    colors = listOf(GsPrimary, GsSecondary),
)

/** Gradient viền sáng mỏng cho Card cao cấp (dùng làm border, không phải fill). */
val GsHairlineGradient = Brush.linearGradient(
    colors = listOf(GsPrimary.copy(alpha = 0.55f), GsSecondary.copy(alpha = 0.25f)),
)
