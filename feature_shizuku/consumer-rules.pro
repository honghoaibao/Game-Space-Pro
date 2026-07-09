# ShellCommandUserService is never referenced from normal app code — it's
# instantiated by Shizuku, in a separate process, by fully-qualified class
# name via reflection (see ShizukuShellExecutor.bindAndAwait). R8 has no
# static call site to see, so without these rules a release build would
# strip or rename it and Shizuku's bind would fail silently at runtime.
-keep class com.gamespacepro.feature.shizuku.service.ShellCommandUserService { *; }
-keep class com.gamespacepro.feature.shizuku.service.IShellCommandService { *; }
-keep class com.gamespacepro.feature.shizuku.service.IShellCommandService$Stub { *; }
-keep class com.gamespacepro.feature.shizuku.service.IShellCommandService$Stub$Proxy { *; }
-keep class com.gamespacepro.feature.shizuku.service.ShellCommandResultParcel { *; }
