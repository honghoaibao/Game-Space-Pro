package com.gamespacepro.feature.shizuku

import android.content.pm.PackageManager
import com.gamespacepro.domain.shell.ShellPermissionManager
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * [ShellPermissionManager] backed by Shizuku's self-implemented permission
 * model (stable since Shizuku API v11).
 *
 * Bridges [Shizuku.requestPermission]'s listener-based callback to a single
 * suspend call so use cases/ViewModels don't manage listener registration
 * lifecycles themselves. The listener removes itself as soon as it fires
 * for this specific request code, so it can't leak or fire twice.
 *
 * NOTE: the exact method name on [Shizuku.OnRequestPermissionResultListener]
 * is transcribed from documentation examples rather than compiled against
 * the real artifact in this sandbox — Android Studio will flag it
 * immediately if it doesn't match (compile error, not a silent bug), but
 * it's the one line in this file I'd check first on a red build.
 */
class ShizukuPermissionManager @Inject constructor() : ShellPermissionManager {

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val requestCode = REQUEST_CODE

        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(code: Int, grantResult: Int) {
                if (code != requestCode) return
                Shizuku.removeRequestPermissionResultListener(this)
                if (continuation.isActive) {
                    continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }

        Shizuku.addRequestPermissionResultListener(listener)
        continuation.invokeOnCancellation {
            Shizuku.removeRequestPermissionResultListener(listener)
        }

        Shizuku.requestPermission(requestCode)
    }

    private companion object {
        // Arbitrary, stable value — only used to correlate the async
        // result callback with this specific request.
        const val REQUEST_CODE = 0x5350 // "SP" — Shizuku Permission
    }
}
