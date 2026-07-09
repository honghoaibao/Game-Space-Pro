# feature_dashboard

The app's home screen — first real UI beyond `MainActivity`'s placeholder.
Shows Shizuku and Accessibility capability status side by side, with an
action to request Shizuku permission or open Accessibility settings.

## Architecture note worth calling out

`DashboardViewModel` depends only on `com.gamespacepro.domain.shell.*` and
`com.gamespacepro.domain.accessibility.*` interfaces — this module's
`build.gradle.kts` depends on `:domain` and `:ui` only, **not** on
`feature_shizuku` or `feature_accessibility`. Their concrete
`ShizukuCapabilityDetector` / `GameSpaceAccessibilityCapabilityDetector`
etc. are bound to those interfaces via Hilt `@Binds` modules that live in
those feature modules, and get wired into the graph because `:app` depends
on all of them. This is the Clean Architecture / DI split from the earlier
scaffolding passes actually paying off: the dashboard was built without
ever importing `rikka.shizuku.*` or `android.accessibilityservice.*`.

## Hilt + Compose: a note on which artifact

Used `androidx.hilt:hilt-navigation-compose` (`hiltViewModel()`) rather
than the newer `androidx.hilt:hilt-lifecycle-viewmodel-compose` artifact
Google introduced to let apps get `hiltViewModel()` without depending on
Navigation Compose. That decoupling doesn't help here — this module needs
Navigation Compose regardless (`:app`'s `NavHost` needs it), and I have
much more confidence in the older artifact's exact API surface than the
newer one, which is recent enough that I'd be guessing at details I
haven't seen broadly documented. The older artifact still works (it
delegates to the new one internally) — this can be revisited if the
project wants to drop the Navigation Compose dependency from a future
screen that doesn't otherwise need it.

## Scope decisions

- **No auto-refresh on resume.** Coming back from Accessibility settings
  (or after Shizuku grants permission) doesn't automatically update the
  cards — the user taps "Refresh". A lifecycle-aware auto-refresh
  (`LifecycleResumeEffect` or similar) is a reasonable follow-up but is UX
  polish, not core functionality, and pulls in yet another lifecycle
  artifact for a first pass.
- **`DashboardViewModel` doesn't wrap the two capability reads in a
  `UseCase`.** They're synchronous, side-effect-free reads with no real
  failure mode — wrapping them in the `UseCase<Params, Result>` base class
  (designed for suspend operations that can fail) would be ceremony, not
  architecture. `onRequestShellPermissionClick` similarly calls
  `ShellPermissionManager` directly rather than through a use case, for the
  same reason.

## Testing

`DashboardViewModelTest` is a genuine, meaningful test — the ViewModel's
only dependencies are the domain interfaces, which are trivial to fake, so
no Robolectric or Android framework needed at all. It's also the test that
caught a real bug while writing it: the first draft of
`onRequestShellPermissionClick` set `isRequestingShellPermission = true`
*inside* the launched coroutine rather than synchronously before launching
it, which meant two rapid clicks could both pass the in-flight guard check
and both fire `requestPermission()`. Fixed before this ever reached the
sandbox verification stage other modules got — this one didn't need a
throwaway JVM harness because the real Gradle module's own test already
exercises it directly.

No screenshot/Compose UI test yet for `DashboardScreen` itself
(`ui-test-junit4` is already in the version catalog from the app module's
setup, just not wired into this module's `androidTest` — worth adding once
there's more than one screen to justify the instrumentation-test
investment).
