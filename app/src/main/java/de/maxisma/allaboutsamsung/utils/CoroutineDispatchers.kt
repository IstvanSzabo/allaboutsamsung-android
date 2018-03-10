package de.maxisma.allaboutsamsung.utils

import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.Executors

val IOPool = Executors.newCachedThreadPool().asCoroutineDispatcher()

/**
 * The dispatcher to be used for database write operations. It is backed by a single thread.
 */
val DbWriteDispatcher = newSingleThreadContext("DbWriter")