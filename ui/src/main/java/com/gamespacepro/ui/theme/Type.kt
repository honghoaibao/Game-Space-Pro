package com.gamespacepro.ui.theme

import androidx.compose.material3.Typography

/**
 * Material 3 default type scale, using the system font family.
 *
 * Deliberately not customized yet: no brand typeface has been supplied, and
 * every screen already reads styles through `MaterialTheme.typography`
 * rather than hardcoding `TextStyle`s — so plugging in a custom font later
 * is a one-file change here, not a find-and-replace across feature modules.
 */
val GameSpaceProTypography = Typography()
