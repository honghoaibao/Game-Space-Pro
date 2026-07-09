package com.gamespacepro.feature.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceEnabledParserTest {

    private val expected = "com.gamespacepro/com.gamespacepro.feature.accessibility.GameSpaceAccessibilityService"

    @Test
    fun `returns false for a null setting`() {
        assertFalse(AccessibilityServiceEnabledParser.isEnabled(null, expected))
    }

    @Test
    fun `returns false for an empty setting`() {
        assertFalse(AccessibilityServiceEnabledParser.isEnabled("", expected))
    }

    @Test
    fun `returns true when the component is the only enabled service`() {
        assertTrue(AccessibilityServiceEnabledParser.isEnabled(expected, expected))
    }

    @Test
    fun `returns true when the component is one of several enabled services`() {
        val raw = "com.other/com.other.SomeService:$expected:com.another/com.another.OtherService"

        assertTrue(AccessibilityServiceEnabledParser.isEnabled(raw, expected))
    }

    @Test
    fun `returns false when the component is not present among others`() {
        val raw = "com.other/com.other.SomeService:com.another/com.another.OtherService"

        assertFalse(AccessibilityServiceEnabledParser.isEnabled(raw, expected))
    }
}
