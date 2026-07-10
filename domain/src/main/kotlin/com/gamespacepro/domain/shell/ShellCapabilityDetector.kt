package com.gamespacepro.domain.shell

/** Detects whether the shell-execution backend is available and authorized. */
interface ShellCapabilityDetector {
    /**
     * Cheap, synchronous check — safe to call before every privileged
     * operation. Implementations must not assume a previous check is
     * still valid; the backend can disappear (Shizuku killed, permission
     * revoked) between calls.
     */
    fun currentCapability(): ShellCapability
}
