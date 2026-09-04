package com.infinitezerone.minibgm.core.common

sealed interface AppResult<out T> {
    data class Success<T>(
        val data: T,
    ) : AppResult<T>

    data class Error(
        val throwable: Throwable,
        val message: String = throwable.message ?: "Unknown error",
    ) : AppResult<Nothing>

    data object Loading : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Error -> this
        is AppResult.Loading -> this
    }

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (Throwable, String) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(throwable, message)
    return this
}
