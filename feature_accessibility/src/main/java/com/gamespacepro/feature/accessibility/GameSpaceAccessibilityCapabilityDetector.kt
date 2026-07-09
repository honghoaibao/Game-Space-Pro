package com.gamespacepro.feature.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.gamespacepro.domain.accessibility.AccessibilityCapability
import com.gamespacepro.domain.accessibility.AccessibilityCapabilityDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GameSpaceAccessibilityCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccessibilityCapabilityDetector {

    override fun currentCapability(): AccessibilityCapability {
        val expectedComponent = ComponentName(context, GameSpaceAccessibilityService::class.java)
            .flattenToString()
        val rawSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )

        return if (AccessibilityServiceEnabledParser.isEnabled(rawSetting, expectedComponent)) {
            AccessibilityCapability.ENABLED
        } else {
            AccessibilityCapability.DISABLED
        }
    }
}
