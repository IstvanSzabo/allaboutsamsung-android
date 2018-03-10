package de.maxisma.allaboutsamsung.utils

import kotlinx.coroutines.experimental.delay
import kotlin.reflect.KClass

/**
 * Automatically retries the execution of [f]. If [maxTries] is reached, the last [Exception] is rethrown.
 *
 * @param handleTypes The exceptions to catch and retry after
 * @param initialBackoffMs The first backoff time. Doubled after every exception.
 * @param maxTries The maximum number of executions of [f]
 */
suspend fun <T> retry(vararg handleTypes: KClass<out Exception>, initialBackoffMs: Long = 500L, maxTries: Int = 5, f: suspend () -> T): T {
    require(maxTries > 0) { "You must allow at least one try." }

    var backoffMs = initialBackoffMs
    lateinit var lastException: Throwable
    repeat(maxTries) {
        try {
            return f()
        } catch (e: Exception) {
            if (handleTypes.any { it.java.isAssignableFrom(e.javaClass) }) {
                e.printStackTrace()
                lastException = e
                delay(backoffMs)
                backoffMs *= 2
            } else {
                throw e
            }
        }
    }
    throw lastException
}