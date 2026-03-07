package com.mock.realteeth.command.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface LockManager {
    suspend fun <T> executeWithLock(
        key: String,
        acquireTimeout: Duration = 0.milliseconds,
        holdTimeout: Duration = Duration.INFINITE,
        action: suspend () -> T,
    ): T
}
