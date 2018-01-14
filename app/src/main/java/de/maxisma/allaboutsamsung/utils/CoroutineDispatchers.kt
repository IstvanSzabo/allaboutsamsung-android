package de.maxisma.allaboutsamsung.utils

import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.Executors

val IOPool = Executors.newCachedThreadPool().asCoroutineDispatcher()