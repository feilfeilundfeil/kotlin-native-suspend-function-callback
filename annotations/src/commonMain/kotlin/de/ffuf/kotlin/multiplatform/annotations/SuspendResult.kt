/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST", "RedundantVisibilityModifier")

package de.ffuf.kotlin.multiplatform.annotations

import kotlin.jvm.JvmField

/**
 * A discriminated union that encapsulates successful outcome with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception.
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@SinceKotlin("1.3")
public class SuspendResult<T> @PublishedApi internal constructor(
    @PublishedApi
    internal val value: Any?
) {
    // discovery

    /**
     * Returns `true` if this instance represents successful outcome.
     * In this case [isFailure] returns `false`.
     */
    public val isSuccess: Boolean get() = value !is Failure

    /**
     * Returns `true` if this instance represents failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    public val isFailure: Boolean get() = value is Failure

    // value & exception retrieval

    /**
     * Returns the encapsulated value if this instance represents [success][SuspendResult.isSuccess] or `null`
     * if it is [failure][SuspendResult.isFailure].
     *
     * This function is shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */
    public fun getOrNull(): T? =
        when {
            isFailure -> null
            else -> value as T
        }

    /**
     * Returns the encapsulated exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    /**
     * Throws exception if the result is failure. This internal function minimizes
     * inlined bytecode for [getOrThrow] and makes sure that in the future we can
     * add some exception-augmenting logic here (if needed).
     */
    @PublishedApi
    @SinceKotlin("1.3")
    internal fun SuspendResult<*>.throwOnFailure() {
        if (value is SuspendResult.Failure) throw value.exception
    }

    /**
     * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
     * catching and encapsulating any thrown exception as a failure.
     */

// -- extensions ---

    /**
     * Returns the encapsulated value if this instance represents [success][SuspendResult.isSuccess] or throws the encapsulated exception
     * if it is [failure][SuspendResult.isFailure].
     *
     * This function is shorthand for `getOrElse { throw it }` (see [getOrElse]).
     */

    @SinceKotlin("1.3")
    public fun getOrThrow(): T {
        throwOnFailure()
        return value as T
    }

    /**
     * Returns the encapsulated value if this instance represents [success][SuspendResult.isSuccess] or the
     * result of [onFailure] function for encapsulated exception if it is [failure][SuspendResult.isFailure].
     *
     * Note, that an exception thrown by [onFailure] function is rethrown by this function.
     *
     * This function is shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
     */

    @SinceKotlin("1.3")
    public fun getOrElse(onFailure: (exception: Throwable) -> T): T {
        return when (val exception = exceptionOrNull()) {
            null -> value as T
            else -> onFailure(exception)
        }
    }

    /**
     * Returns the encapsulated value if this instance represents [success][SuspendResult.isSuccess] or the
     * [defaultValue] if it is [failure][SuspendResult.isFailure].
     *
     * This function is shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
     */

    /*@SinceKotlin("1.3")
    public fun getOrDefault(defaultValue: T): T {
        if (isFailure) return defaultValue
        return value as R
    }*/ //TODO

    /**
     * Returns the the result of [onSuccess] for encapsulated value if this instance represents [success][SuspendResult.isSuccess]
     * or the result of [onFailure] function for encapsulated exception if it is [failure][SuspendResult.isFailure].
     *
     * Note, that an exception thrown by [onSuccess] or by [onFailure] function is rethrown by this function.
     */

    @SinceKotlin("1.3")
    public fun fold(
        onSuccess: (value: T) -> T,
        onFailure: (exception: Throwable) -> T
    ): T {
        return when (val exception = exceptionOrNull()) {
            null -> onSuccess(value as T)
            else -> onFailure(exception)
        }
    }

// transformation

    /**
     * Returns the encapsulated result of the given [transform] function applied to encapsulated value
     * if this instance represents [success][SuspendResult.isSuccess] or the
     * original encapsulated exception if it is [failure][SuspendResult.isFailure].
     *
     * Note, that an exception thrown by [transform] function is rethrown by this function.
     * See [mapCatching] for an alternative that encapsulates exceptions.
     */

    @SinceKotlin("1.3")
    public fun map(transform: (value: T) -> T): SuspendResult<T> {
        return when {
            isSuccess -> SuspendResult.success(transform(value as T))
            else -> SuspendResult(value)
        }
    }

    /**
     * Returns the encapsulated result of the given [transform] function applied to encapsulated value
     * if this instance represents [success][SuspendResult.isSuccess] or the
     * original encapsulated exception if it is [failure][SuspendResult.isFailure].
     *
     * Any exception thrown by [transform] function is caught, encapsulated as a failure and returned by this function.
     * See [map] for an alternative that rethrows exceptions.
     */

    @SinceKotlin("1.3")
    public suspend fun mapCatching(transform: (value: T) -> T): SuspendResult<T> {
        return when {
            isSuccess -> suspendRunCatching<T> { transform(value as T) }
            else -> SuspendResult(value)
        }
    }

    /**
     * Returns the encapsulated result of the given [transform] function applied to encapsulated exception
     * if this instance represents [failure][SuspendResult.isFailure] or the
     * original encapsulated value if it is [success][SuspendResult.isSuccess].
     *
     * Note, that an exception thrown by [transform] function is rethrown by this function.
     * See [recoverCatching] for an alternative that encapsulates exceptions.
     */

    @SinceKotlin("1.3")
    public fun recover(transform: (exception: Throwable) -> T): SuspendResult<T> {
        return when (val exception = exceptionOrNull()) {
            null -> this
            else -> SuspendResult.success(transform(exception))
        }
    }

    /**
     * Returns the encapsulated result of the given [transform] function applied to encapsulated exception
     * if this instance represents [failure][SuspendResult.isFailure] or the
     * original encapsulated value if it is [success][SuspendResult.isSuccess].
     *
     * Any exception thrown by [transform] function is caught, encapsulated as a failure and returned by this function.
     * See [recover] for an alternative that rethrows exceptions.
     */

    @SinceKotlin("1.3")
    public suspend fun recoverCatching(transform: (exception: Throwable) -> T): SuspendResult<T> {
        val value = value // workaround for classes BE bug
        return when (val exception = exceptionOrNull()) {
            null -> this
            else -> suspendRunCatching<T> { transform(exception) }
        }
    }

// "peek" onto value/exception and pipe

    /**
     * Performs the given [action] on encapsulated exception if this instance represents [failure][SuspendResult.isFailure].
     * Returns the original `Result` unchanged.
     */

    @SinceKotlin("1.3")
    public fun onFailure(action: (exception: Throwable) -> Unit): SuspendResult<T> {
        exceptionOrNull()?.let { action(it) }
        return this
    }

    /**
     * Performs the given [action] on encapsulated value if this instance represents [success][SuspendResult.isSuccess].
     * Returns the original `Result` unchanged.
     */

    @SinceKotlin("1.3")
    public fun onSuccess(action: (value: T) -> Unit): SuspendResult<T> {
        if (isSuccess) action(value as T)
        return this
    }

// -------------------


    /**
     * Returns a string `Success(v)` if this instance represents [success][SuspendResult.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    public override fun toString(): String =
        when (value) {
            is Failure -> value.toString() // "Failure($exception)"
            else -> "Success($value)"
        }

    // companion with constructors

    /**
     * Companion object for [SuspendResult] class that contains its constructor functions
     * [success] and [failure].
     */
    public companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */

        public fun <T> success(value: T): SuspendResult<T> =
            SuspendResult(value)

        /**
         * Returns an instance that encapsulates the given [exception] as failure.
         */

        public fun <T> failure(exception: Throwable): SuspendResult<T> =
            SuspendResult(createFailure(exception))
    }

    internal class Failure(
        @JvmField
        val exception: Throwable
    ) {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}

/**
 * Creates an instance of internal marker [SuspendResult.Failure] class to
 * make sure that this class is not exposed in ABI.
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun createFailure(exception: Throwable): Any =
    SuspendResult.Failure(exception)

@SinceKotlin("1.3")
public suspend inline fun <R> suspendRunCatching(crossinline block: suspend () -> R): SuspendResult<R> {
    return try {
        SuspendResult.success(block())
    } catch (e: Throwable) {
        SuspendResult.failure(e)
    }
}

/**
 * Calls the specified function [block] with `this` value as its receiver and returns its encapsulated result
 * if invocation was successful, catching and encapsulating any thrown exception as a failure.
 */

@SinceKotlin("1.3")
public inline fun <T> T.suspendRunCatching(block: T.() -> T): SuspendResult<T> {
    return try {
        SuspendResult.success(block())
    } catch (e: Throwable) {
        SuspendResult.failure(e)
    }
}
