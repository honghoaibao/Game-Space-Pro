package com.gamespacepro.domain.accessibility

import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes the package name of the app currently in the foreground, as
 * detected by the accessibility service. `null` means unknown — either
 * nothing has been observed yet, or the service isn't currently enabled.
 */
interface ForegroundAppObserver {
    val foregroundApp: StateFlow<String?>
}
