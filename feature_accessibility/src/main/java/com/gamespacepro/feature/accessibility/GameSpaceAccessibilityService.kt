package com.gamespacepro.feature.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.gamespacepro.core.di.MainImmediateDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Per the product spec, this service is deliberately narrow: it detects
 * the foreground app and nothing else. Dialog/permission-window detection
 * and "trigger lightweight actions" from the spec are NOT implemented yet
 * — see feature_accessibility/README.md for why.
 */
@AndroidEntryPoint
class GameSpaceAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var foregroundAppRepository: ForegroundAppRepository

    @Inject
    @MainImmediateDispatcher
    lateinit var mainImmediateDispatcher: CoroutineDispatcher

    private lateinit var serviceScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Hilt injects @Inject fields right after super.onCreate() returns,
        // so mainImmediateDispatcher isn't available until this point —
        // this can't be a property initializer above.
        serviceScope = CoroutineScope(SupervisorJob() + mainImmediateDispatcher)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("GameSpaceAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // This callback runs on the main thread as part of the system's
        // accessibility event pipeline and must return immediately — hand
        // off even this lightweight update through a coroutine per the
        // product spec's "heavy logic must run in coroutines" requirement,
        // establishing the pattern any future, genuinely heavier handler
        // added to this class should follow.
        serviceScope.launch {
            Timber.d("Foreground app changed: %s", packageName)
            foregroundAppRepository.onForegroundAppChanged(packageName)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        foregroundAppRepository.onForegroundAppChanged(null)
        if (::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
        super.onDestroy()
    }
}
