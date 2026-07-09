package com.gamespacepro.domain.result

/**
 * Result wrapper returned by every [com.gamespacepro.domain.usecase.UseCase]
 * invocation.
 *
 * Kept to exactly two states on purpose. A "Loading" state is intentionally
 * NOT modeled here: loading/progress is presentation-layer UI state, derived
 * from *starting* a use case call, not part of the use case's own result.
 * Mixing the two tends to leak ViewModel concerns into the domain layer.
 */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val exception: Throwable) : AppResult<Nothing>()
}

/** Runs [action] with the success value, if present, and returns the receiver unchanged. */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/** Runs [action] with the failure cause, if present, and returns the receiver unchanged. */
inline fun <T> AppResult<T>.onError(action: (Throwable) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(exception)
    return this
}

/** Returns the success value, or `null` if this is an [AppResult.Error]. */
fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
