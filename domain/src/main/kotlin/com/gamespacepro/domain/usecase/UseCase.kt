package com.gamespacepro.domain.usecase

import com.gamespacepro.domain.result.AppResult
import kotlinx.coroutines.CancellationException

/**
 * Base contract for a single-responsibility domain operation.
 *
 * Every use case in Game Space Pro (capability checks, profile application,
 * shell command execution, etc.) extends this instead of exposing repository
 * calls directly to the presentation layer. This keeps business rules —
 * including the safety requirements from the product spec, such as "restore
 * original value on exit/crash" — inside the domain layer, testable in pure
 * Kotlin without Android instrumentation.
 *
 * @param Params input required to execute the use case. Use [Unit] when a
 *   use case takes no parameters.
 * @param Result the successful output type.
 */
abstract class UseCase<in Params, out Result> {

    suspend operator fun invoke(params: Params): AppResult<Result> = try {
        AppResult.Success(execute(params))
    } catch (cancellation: CancellationException) {
        // Never swallow cancellation — coroutines rely on it propagating
        // up the call stack to cancel sibling/parent work correctly.
        throw cancellation
    } catch (exception: Exception) {
        AppResult.Error(exception)
    }

    protected abstract suspend fun execute(params: Params): Result
}
