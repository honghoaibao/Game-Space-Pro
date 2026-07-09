package com.gamespacepro.domain.shell

/** Requests the permission needed to use the shell-execution backend. */
interface ShellPermissionManager {
    /**
     * Suspends until the user responds to the permission request (or the
     * request otherwise resolves). Returns `true` only if permission was
     * granted. Safe to call even when [ShellCapabilityDetector] already
     * reports [ShellCapability.PERMISSION_DENIED] — the implementation
     * decides whether that's still worth prompting for.
     */
    suspend fun requestPermission(): Boolean
}
