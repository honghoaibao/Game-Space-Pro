# feature_shizuku

Wraps the [Shizuku](https://github.com/RikkaApps/Shizuku-API) client library
(`dev.rikka.shizuku:api:13.1.5`) behind the domain-layer contracts in
`com.gamespacepro.domain.shell` — `ShellCapabilityDetector`,
`ShellPermissionManager`, `ShellExecutor`. Nothing outside this module
imports `rikka.shizuku.*` directly; everything else depends on the domain
interfaces, so a root-based or different backend could be swapped in later
without touching any caller.

## What's implemented

- **`ShizukuCapabilityDetector`** — `Shizuku.pingBinder()` /
  `Shizuku.isPreV11()` / `Shizuku.checkSelfPermission()` /
  `Shizuku.shouldShowRequestPermissionRationale()` → `ShellCapability`.
- **`ShizukuPermissionManager`** — bridges `Shizuku.requestPermission()`'s
  listener callback to a single `suspend fun requestPermission(): Boolean`.
- **`ShizukuBindingsModule`** — Hilt `@Binds` wiring for both of the above.
- Manifest: `moe.shizuku.manager.permission.API` permission +
  `rikka.shizuku.ShizukuProvider` declaration, so any app depending on this
  module gets both merged in automatically.

Both classes were written against API usage confirmed across the official
`RikkaApps/Shizuku-API` repo and several independent third-party
walkthroughs — not from memory alone, given how narrow/version-sensitive
this library's surface is.

**One non-obvious thing worth flagging explicitly:**
`Shizuku.shouldShowRequestPermissionRationale()` returning `true` means the
user picked "deny and don't ask again" — the *opposite* of what the
same-named method means on `Activity`. `ShizukuCapabilityDetector`'s branch
order follows Shizuku's own reference implementation, not the intuitive
reading of the method name. Worth remembering if this ever gets refactored.

## The executor — now implemented, but the least-verified part of this module

`ShellExecutor` is now backed by `ShizukuShellExecutor`, using
`Shizuku.bindUserService()` — not `Shizuku.newProcess()`, which is
deprecated and being removed as of the `13.1.x` line this project is
pinned to (confirmed via the official changelog and a live GitHub issue of
a user already hitting it being inaccessible).

**Pieces involved:**

- `IShellCommandService.aidl` / `ShellCommandResultParcel.aidl` +
  `ShellCommandResultParcel.kt` (`@Parcelize`) — the AIDL contract and the
  Parcelable that crosses it. `@Parcelize` is used specifically to avoid
  hand-written `writeToParcel`/`CREATOR` boilerplate, a common source of
  subtle bugs (field-order mismatches) in manually-written Parcelables.
- `ShellCommandUserService` — runs in a separate process under the
  shell/root UID, spawned by Shizuku (not a normal `Service`, not declared
  in any manifest — Shizuku loads it by class name via reflection). Drains
  stdout and stderr on separate threads before calling `waitFor()`, since
  sequential reads can deadlock once either pipe's OS buffer fills.
- `ShizukuShellExecutor` — binds the service once and caches the
  connection (rebinding per-command would add real per-call latency),
  invalidates the cache on a dead binder or disconnection, and bridges the
  callback-based bind API to a suspend call the same way
  `ShizukuPermissionManager` does for permissions.
- `consumer-rules.pro` — keeps `ShellCommandUserService` and the AIDL
  types alive under R8. Worth calling out on its own: this class is never
  referenced from normal code (Shizuku instantiates it reflectively by
  name in a different process), so R8 has no call site to see and would
  silently strip or rename it in a release build — something that would
  work perfectly in every debug build and only break once released.

**One correctness catch from writing this:** the `ComponentName` passed to
`Shizuku.bindUserService` must use the *final app's* package name, not this
library module's own namespace. `BuildConfig.APPLICATION_ID` inside an
Android *library* module resolves to that library's own namespace
(`com.gamespacepro.feature.shizuku`), not the consuming app's
`applicationId` — using it here would point `ComponentName` at a package
that doesn't exist on the device. `ShizukuShellExecutor` uses
`context.packageName` instead, which is correct regardless of which module
the code lives in. Same reasoning is why `debuggable(...)` reads
`context.applicationInfo.flags` rather than `BuildConfig.DEBUG`.

**What's still genuinely unverified**, because it can't be checked without
a real device with Shizuku installed:

- The whole bind → `ServiceConnection` → AIDL call round-trip has never
  actually executed. The individual pieces (AIDL codegen, `ProcessBuilder`,
  thread-based stream draining, `suspendCancellableCoroutine` bridging) are
  each standard and low-risk on their own; what I can't verify is their
  *composition* end-to-end.
- No timeout on `process.waitFor()` — a hung command blocks that call
  indefinitely. Deliberately not adding ad-hoc timeout handling here;
  that's squarely `RecoveryManager` territory (from the product spec) and
  deserves its own design rather than a bolted-on `Thread.join(timeoutMs)`.
- `destroy()` is defined but nothing calls it yet — no lifecycle hook
  (e.g. app backgrounded, Settings toggle to "disconnect Shizuku") decides
  when a client should proactively tear down the service process.
- `RecoveryManager` and `ResultParser` from the product spec are still not
  implemented. Result parsing is likely more natural per-use-case (turning
  raw stdout into whatever a specific command's caller needs) than a single
  shared class — worth deciding when the first real use case
  (`feature_optimizer`, most likely) needs it, rather than guessing at a
  shape now.

**First things to check on a real device if this misbehaves:** the AIDL
method name / `Stub`/`Stub.Proxy` inner-class names in the ProGuard rules
(confirm they match what the AIDL compiler actually generates), and
whether Shizuku's reflection-based instantiation of `ShellCommandUserService`
needs the no-arg constructor to be explicit `public` (Kotlin's default
constructor visibility should already be `public`, but worth a second look
if the bind silently never calls back).

**One thing I did verify, not just reason about:** the concurrent
stdout/stderr draining is the one part of this whole module most likely to
have a subtle bug (sequential draining deadlocks once either pipe's OS
buffer fills — easy to write, easy to get subtly wrong, hard to notice in
casual testing since small outputs never trigger it). I extracted that
logic into `ShellCommandRunner` (no Android dependency) specifically so it
could be compiled and actually run — I installed a JDK and `kotlinc` in my
own sandbox and ran it against a script producing 20,000 lines
simultaneously on both stdout and stderr (well past the ~64KB pipe
buffers that would deadlock sequential draining). It completed in 182ms
with all output and the correct exit code captured. `ShellCommandRunnerTest`
in this module runs the same scenario as a normal JUnit test going forward.
That's real signal on the algorithm; it's not a substitute for testing the
Shizuku/AIDL/Binder plumbing around it end-to-end, which still needs an
actual device.

## Testing

Neither `ShizukuCapabilityDetector`, `ShizukuPermissionManager`, nor
`ShizukuShellExecutor` has a unit test. All three are thin adapters over
`Shizuku`'s static methods / Binder calls, which need a real Binder
connection — calling them in a plain JVM test either throws or silently
returns meaningless defaults, since there's no Shizuku service to actually
talk to. Testing these meaningfully needs either Robolectric (with a
Shizuku-aware shadow, if one exists) or an `androidTest` on a real
device/emulator with Shizuku installed — a bigger decision (new test infra,
and the CI workflow doesn't run instrumentation tests yet) I didn't want to
make unilaterally by quietly adding a dependency.

What IS tested with plain JUnit, because it doesn't need Android at all:

- `ShellCommandRunnerTest` — the actual process-execution algorithm
  (success/failure/exit-code capture, the empty-command guard, a
  can't-start-the-process guard, and the large-concurrent-output
  deadlock scenario described above).
- `ShellCommandResultMapperTest` — the Parcel-to-domain-result mapping,
  including the defensive case where `isSuccess=true` but `exitCode` is
  somehow null.
