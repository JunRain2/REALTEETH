package com.mock.realteeth.infra.lock

import com.mock.realteeth.command.domain.LockManager
import com.mock.realteeth.command.domain.exception.BusinessException
import com.mock.realteeth.command.domain.exception.ErrorCode
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withTimeout
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Component
class MySqlLockManager(
    @Qualifier("namedLockConnectionFactory")
    private val connectionFactory: ConnectionFactory,
) : LockManager {
    override suspend fun <T> executeWithLock(
        key: String,
        acquireTimeout: Duration,
        holdTimeout: Duration,
        action: suspend () -> T,
    ): T {
        val acquireTimeoutSeconds = acquireTimeout.toDouble(DurationUnit.SECONDS)
        val connection = Mono.from(connectionFactory.create()).awaitSingle()
        try {
            val lockResult =
                Mono
                    .from(
                        connection
                            .createStatement("SELECT GET_LOCK(?, ?)")
                            .bind(0, key)
                            .bind(1, acquireTimeoutSeconds)
                            .execute(),
                    ).flatMap { result ->
                        Mono.from(result.map { row, _ -> row.get(0, Int::class.java) })
                    }.awaitSingleOrNull()

            if (lockResult != 1) {
                throw BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED)
            }

            return try {
                withTimeout(holdTimeout) { action() }
            } finally {
                Mono
                    .from(
                        connection
                            .createStatement("SELECT RELEASE_LOCK(?)")
                            .bind(0, key)
                            .execute(),
                    ).awaitSingleOrNull()
            }
        } finally {
            Mono.from(connection.close()).awaitSingleOrNull()
        }
    }
}
