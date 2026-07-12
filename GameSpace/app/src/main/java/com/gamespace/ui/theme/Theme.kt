package com.gamespace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = GsBluePrimary,
    secondary = GsPurpleAccent,
    background = GsBackgroundDark,
    surface = GsSurfaceDark,
    onBackground = GsOnSurfaceDark,
    onSurface = GsOnSurfaceDark,
)

private val LightColors = lightColorScheme(
    primary = GsBluePrimary,
    secondary = GsPurpleAccent,
)

@Composable
fun GameSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
