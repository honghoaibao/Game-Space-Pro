package com.gamespacepro.domain.shell

/**
 * Coarse-grained state of the device's shell-execution backend. The domain
 * layer doesn't know or care that Shizuku is behind it today — matches the
 * product spec's "detect capability first, never assume availability,
 * always provide graceful fallback" requirement.
 */
enum class ShellCapability {
    /** Backend not installed, not running, or otherwise unreachable. */
    UNAVAILABLE,

    /** Backend reachable; permission has not been granted yet. */
    PERMISSION_REQUIRED,

    /** Backend reachable; the user has permanently denied the permission. */
    PERMISSION_DENIED,

    /** Backend reachable and authorized — commands can be executed. */
    READY,
}
