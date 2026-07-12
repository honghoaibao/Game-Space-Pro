package com.gamespace.overlay

data class FloatingBrowserState(
    val currentUrl: String = DEFAULT_URL,
    val addressBarText: String = DEFAULT_URL,
    val isMinimized: Boolean = false,
    val isLocked: Boolean = false,
    val isTouchGuardActive: Boolean = false,
    val alpha: Float = 1f,
    val canGoBack: Boolean = false,
) {
    companion object {
        const val DEFAULT_URL = "https://www.google.com"
        val ALPHA_LEVELS = listOf(1f, 0.75f, 0.5f)
    }
}
