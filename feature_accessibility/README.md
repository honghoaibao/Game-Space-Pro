# feature_accessibility

Implements only the first bullet of the product spec's Accessibility
section — **detect foreground app** — behind
`com.gamespacepro.domain.accessibility` contracts (`ForegroundAppObserver`,
`AccessibilityCapabilityDetector`). Nothing outside this module needs to
know an `AccessibilityService` is involved.

## What's implemented

- **`GameSpaceAccessibilityService`** — listens for `TYPE_WINDOW_STATE_CHANGED`
  only, hands the package name off through a coroutine (see the comment in
  that file for why, even though the update itself is cheap), and pushes it
  into `ForegroundAppRepository`.
- **`ForegroundAppRepository`** — `@Singleton` `StateFlow<String?>` holder,
  bridges the service's callback-driven lifecycle to something any
  Hilt-injected class can observe independent of whether the service is
  currently connected.
- **`GameSpaceAccessibilityCapabilityDetector`** — checks
  `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` for this service's
  component. The actual colon-separated-string parsing is factored out into
  `AccessibilityServiceEnabledParser` (plain Kotlin, no `Settings`/`Context`)
  specifically so it's unit-testable — same reasoning as
  `ShellCommandRunner` in feature_shizuku.
- Manifest: `<service>` + `BIND_ACCESSIBILITY_SERVICE` + the XML config +
  the required user-facing label/description strings.

This is all standard, stable, official Android SDK (`AccessibilityService`,
`Settings.Secure`, Hilt's documented support for `@AndroidEntryPoint` on
`Service`) — much lower API-risk than feature_shizuku's third-party AIDL
surface. The main open question here isn't "is this API right" so much as
"is this the right scope."

## What's deliberately NOT implemented

- **Dialog detection, permission-window detection, "trigger lightweight
  actions"** — the rest of the product spec's Accessibility section. These
  need a concrete downstream consumer to design against sensibly: what
  counts as "a dialog" worth reacting to, and what action should actually
  fire, are questions that belong to whichever feature needs them
  (automation engine, most likely) rather than being guessed at here.
  Bolting on a generic "detect any dialog-like window" heuristic now would
  be exactly the kind of speculative, hard-to-validate feature this project
  has been avoiding elsewhere (see the Room/ShellExecutor-RecoveryManager
  deferrals in the other modules' READMEs).
- **`android:canRetrieveWindowContent="false"`** in the service config — on
  purpose. This service currently only reads `AccessibilityEvent.packageName`,
  which doesn't need window content access at all. Flip this to `true` (and
  add `flagRetrieveInteractiveWindows`) only when dialog detection actually
  needs it — no reason to declare a more invasive capability than what's
  in use.
- **No enable/disable flow in the UI yet** — `AccessibilityCapabilityDetector`
  exists so a future settings screen can check state and deep-link to
  `Settings.ACTION_ACCESSIBILITY_SETTINGS`, but nothing calls it yet since
  there's no UI consuming it.

## Testing

`AccessibilityServiceEnabledParserTest` and `ForegroundAppRepositoryTest`
are both real, meaningful unit tests — neither needs Android at all.
`GameSpaceAccessibilityService` and `GameSpaceAccessibilityCapabilityDetector`
themselves aren't tested, for the same reason as feature_shizuku's
Shizuku-backed classes: they're thin wrappers around real
`AccessibilityService`/`Settings.Secure` calls that need Robolectric or an
`androidTest` on a device to exercise meaningfully.
