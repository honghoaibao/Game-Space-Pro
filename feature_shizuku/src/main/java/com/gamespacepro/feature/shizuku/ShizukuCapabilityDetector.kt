package com.gamespacepro.feature.shizuku

import android.content.pm.PackageManager
import com.gamespacepro.domain.shell.ShellCapability
import com.gamespacepro.domain.shell.ShellCapabilityDetector
import rikka.shizuku.Shizuku
import javax.inject.Inject

/**
 * [ShellCapabilityDetector] backed by the Shizuku client library.
 *
 * Note the branch order below matches Shizuku's own reference
 * implementation, not the "obvious" reading of the method names:
 * [Shizuku.shouldShowRequestPermissionRationale] returning `true` means the
 * user picked "deny and don't ask again" — the *opposite* of what the
 * same-named method means on [android.app.Activity]. Getting this backwards
 * would silently keep re-prompting a user who already permanently denied
 * the permission.
 */
class ShizukuCapabilityDetector @Inject constructor() : ShellCapabilityDetector {

    override fun currentCapability(): ShellCapability {
        if (!Shizuku.pingBinder()) {
            return ShellCapability.UNAVAILABLE
        }
        if (Shizuku.isPreV11()) {
            // Pre-v11 predates the self-implemented permission model this
            // class relies on below; treat it the same as "not installed"
            // rather than special-casing a legacy permission flow.
            return ShellCapability.UNAVAILABLE
        }
        return when {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShellCapability.READY
            Shizuku.shouldShowRequestPermissionRationale() -> ShellCapability.PERMISSION_DENIED
            else -> ShellCapability.PERMISSION_REQUIRED
        }
    }
}
