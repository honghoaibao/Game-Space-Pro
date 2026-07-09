package com.gamespacepro.feature.accessibility

/**
 * Parses Android's colon-separated `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
 * string. Factored out as plain Kotlin (not `android.text.TextUtils`,
 * which does the same job but is stubbed in plain JVM unit tests) so this
 * specific logic is testable without Robolectric or a real ContentResolver.
 */
internal object AccessibilityServiceEnabledParser {

    fun isEnabled(rawEnabledServicesSetting: String?, expectedFlattenedComponent: String): Boolean {
        if (rawEnabledServicesSetting.isNullOrEmpty()) return false

        return rawEnabledServicesSetting
            .split(':')
            .any { it.equals(expectedFlattenedComponent, ignoreCase = true) }
    }
}
