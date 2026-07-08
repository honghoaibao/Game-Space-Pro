package com.gamespacepro.feature.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.os.IBinder
import com.gamespacepro.core.di.IoDispatcher
import com.gamespacepro.domain.shell.ShellCapability
import com.gamespacepro.domain.shell.ShellCapabilityDetector
import com.gamespacepro.domain.shell.ShellCommandResult
import com.gamespacepro.domain.shell.ShellExecutor
import com.gamespacepro.feature.shizuku.service.IShellCommandService
import com.gamespacepro.feature.shizuku.service.ShellCommandUserService
import com.gamespacepro.feature.shizuku.service.toDomainResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val SERVICE_VERSION = 1

/**
 * [ShellExecutor] backed by a Shizuku
 * [UserService][Shizuku.bindUserService] running [ShellCommandUserService].
 *
 * The binding is established once and cached, not re-established per
 * command — binding has real latency (it's a cross-process call). A dead
 * binder (service process killed, Shizuku restarted) invalidates the cache
 * and the next call re-binds.
 */
@Singleton
class ShizukuShellExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDetector: ShellCapabilityDetector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ShellExecutor {

    private val connectionMutex = Mutex()

    @Volatile
    private var cachedService: IShellCommandService? = null

    override suspend fun execute(command: List<String>): ShellCommandResult {
        if (command.isEmpty()) {
            return ShellCommandResult.Failure(
                exitCode = null,
                stdout = "",
                stderr = "",
                executionTimeMillis = 0,
                cause = IllegalArgumentException("command must not be empty"),
            )
        }

        if (capabilityDetector.currentCapability() != ShellCapability.READY) {
            return ShellCommandResult.Failure(
                exitCode = null,
                stdout = "",
                stderr = "",
                executionTimeMillis = 0,
                cause = IllegalStateException("Shizuku is not ready to execute commands"),
            )
        }

        val startTime = System.currentTimeMillis()
        return try {
            val service = obtainService()
            val parcel = withContext(ioDispatcher) {
                service.runCommand(command.toTypedArray())
            }
            parcel.toDomainResult()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            // Most likely a dead/disconnected binder between obtainService()
            // and the call actually landing — don't keep handing out a
            // known-bad cached reference.
            cachedService = null
            ShellCommandResult.Failure(
                exitCode = null,
                stdout = "",
                stderr = "",
                executionTimeMillis = System.currentTimeMillis() - startTime,
                cause = exception,
            )
        }
    }

    private suspend fun obtainService(): IShellCommandService {
        cachedService?.let { existing -> if (existing.asBinder().isBinderAlive) return existing }

        return connectionMutex.withLock {
            cachedService?.let { existing -> if (existing.asBinder().isBinderAlive) return@withLock existing }
            bindAndAwait().also { cachedService = it }
        }
    }

    private suspend fun bindAndAwait(): IShellCommandService = suspendCancellableCoroutine { continuation ->
        // IMPORTANT: context.packageName, not BuildConfig.APPLICATION_ID.
        // BuildConfig.APPLICATION_ID inside a *library* module (this one)
        // resolves to this module's own namespace
        // (com.gamespacepro.feature.shizuku), not the final app's
        // applicationId — using it here would build a ComponentName
        // pointing at a package that doesn't exist on the device.
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellCommandUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("shell_service")
            .debuggable(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            .version(SERVICE_VERSION)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder == null || !binder.pingBinder()) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("Shizuku user service connected with a dead binder"),
                        )
                    }
                    return
                }
                if (continuation.isActive) {
                    continuation.resume(IShellCommandService.Stub.asInterface(binder))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                cachedService = null
            }
        }

        Shizuku.bindUserService(args, connection)
        continuation.invokeOnCancellation {
            Shizuku.unbindUserService(args, connection, true)
        }
    }
}
