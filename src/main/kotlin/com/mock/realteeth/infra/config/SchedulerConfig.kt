package com.mock.realteeth.infra.config

import io.r2dbc.spi.ConnectionFactory
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.r2dbc.R2dbcLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
class SchedulerConfig(
    @param:Value("\${app.scheduler.task-scheduler.pool-size}") private val poolSize: Int,
    @param:Value("\${app.scheduler.task-scheduler.await-termination-seconds}") private val awaitTerminationSeconds: Int,
) {
    @Bean
    fun lockProvider(connectionFactory: ConnectionFactory): LockProvider = R2dbcLockProvider(connectionFactory)

    @Bean
    fun taskScheduler(): TaskScheduler {
        val size = poolSize
        val terminationSeconds = awaitTerminationSeconds
        return ThreadPoolTaskScheduler().apply {
            poolSize = size
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(terminationSeconds)
            initialize()
        }
    }
}
