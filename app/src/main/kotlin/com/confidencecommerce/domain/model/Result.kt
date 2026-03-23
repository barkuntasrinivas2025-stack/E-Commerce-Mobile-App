package com.confidencecommerce.domain.model

/**
 * Typed result wrapper used across all layers.
 * Prevents exceptions from leaking through layer boundaries.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val cause: Throwable? = null
    ) : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppResult.Error) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(this)
    return this
}

fun <T> AppResult<T>.dataOrNull(): T? = (this as? AppResult.Success)?.data
