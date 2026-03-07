package com.mock.realteeth.infra.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class NamedLockConfig(
    private val r2dbcProperties: R2dbcProperties,
    @Value("\${app.named-lock.pool-size:5}") private val poolSize: Int,
    @Value("\${app.named-lock.initial-size:1}") private val initialSize: Int,
    @Value("\${app.named-lock.max-idle-time-minutes:10}") private val maxIdleTimeMinutes: Long,
    @Value("\${app.named-lock.max-acquire-time-seconds:30}") private val maxAcquireTimeSeconds: Long,
) {
    @Bean("namedLockConnectionFactory")
    fun namedLockConnectionFactory(): ConnectionPool {
        val options =
            ConnectionFactoryOptions
                .parse(r2dbcProperties.url)
                .mutate()
                .option(ConnectionFactoryOptions.USER, r2dbcProperties.username)
                .option(
                    ConnectionFactoryOptions.PASSWORD,
                    r2dbcProperties.password ?: error("spring.r2dbc.password is required"),
                ).build()

        val poolConfig =
            ConnectionPoolConfiguration
                .builder(ConnectionFactories.get(options))
                .initialSize(initialSize)
                .maxSize(poolSize)
                .maxIdleTime(Duration.ofMinutes(maxIdleTimeMinutes))
                .maxAcquireTime(Duration.ofSeconds(maxAcquireTimeSeconds))
                .build()

        return ConnectionPool(poolConfig)
    }
}
