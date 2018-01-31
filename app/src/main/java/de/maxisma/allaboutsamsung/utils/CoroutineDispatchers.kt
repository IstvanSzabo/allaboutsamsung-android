package de.maxisma.allaboutsamsung.utils

import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.Executors

val IOPool = Executors.newCachedThreadPool().asCoroutineDispatcher()
val DbWriteDispatcher = newSingleThreadContext("DbWriter")