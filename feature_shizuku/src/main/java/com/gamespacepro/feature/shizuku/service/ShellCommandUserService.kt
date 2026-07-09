package com.gamespacepro.feature.shizuku.service

/**
 * Runs as a separate process under the shell/root UID, spawned by Shizuku
 * — this is NOT a normal Android [android.app.Service] and is not declared
 * in any manifest; Shizuku instantiates it by class name via
 * [Shizuku.bindUserService][rikka.shizuku.Shizuku.bindUserService] on the
 * client side. No-arg constructor only — see feature_shizuku/README.md for
 * why the (Context) constructor overload Shizuku v13+ also supports is
 * deliberately not used here.
 *
 * The actual process-running logic lives in [ShellCommandRunner], factored
 * out specifically so it can be unit tested on a plain JVM — this class
 * itself extends an AIDL-generated [android.os.Binder] subclass, which
 * can't be instantiated outside Robolectric/instrumentation.
 */
class ShellCommandUserService : IShellCommandService.Stub() {

    override fun runCommand(command: Array<String>): ShellCommandResultParcel =
        ShellCommandRunner.run(command.toList())

    override fun destroy() {
        System.exit(0)
    }
}
