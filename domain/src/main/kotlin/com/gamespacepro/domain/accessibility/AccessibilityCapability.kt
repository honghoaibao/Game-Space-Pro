package com.gamespacepro.domain.accessibility

/**
 * Unlike Shizuku's [com.gamespacepro.domain.shell.ShellCapability], there's
 * no separate "not installed" or "permission requested but not yet
 * answered" state — the accessibility service ships inside this app, and
 * enabling it in system Settings both installs and authorizes it in one
 * step.
 */
enum class AccessibilityCapability {
    /** The user has not enabled the service in system Settings. */
    DISABLED,

    /** The service is enabled and (assuming the OS has connected it) receiving events. */
    ENABLED,
}
