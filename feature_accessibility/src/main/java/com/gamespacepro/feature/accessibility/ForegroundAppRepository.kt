package com.gamespacepro.feature.accessibility

import com.gamespacepro.domain.accessibility.ForegroundAppObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges [GameSpaceAccessibilityService]'s event callbacks — which only
 * exist while the OS has the service connected — to a [StateFlow] any
 * Hilt-injected class can hold onto and observe regardless of the
 * service's lifecycle.
 */
@Singleton
class ForegroundAppRepository @Inject constructor() : ForegroundAppObserver {

    private val _foregroundApp = MutableStateFlow<String?>(null)

    override val foregroundApp: StateFlow<String?> = _foregroundApp.asStateFlow()

    /** Called by [GameSpaceAccessibilityService] on every foreground-app change. */
    fun onForegroundAppChanged(packageName: String?) {
        _foregroundApp.value = packageName
    }
}
