package com.mock.realteeth.infra.scheduler

import com.mock.realteeth.command.application.ToothAnalysisService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class PendingAnalysisScheduler(
    private val commandService: ToothAnalysisService,
    private val circuitBreaker: CircuitBreaker,
) {
    @Scheduled(fixedDelayString = "\${app.scheduler.pending-analysis.fixed-delay-ms:10000}")
    @SchedulerLock(name = "pendingAnalysisScheduler", lockAtMostFor = "\${app.scheduler.pending-analysis.lock-at-most-for:PT30S}")
    fun processPending() {
        log.info { "[PendingAnalysisScheduler] started" }
        when (circuitBreaker.state) {
            CircuitBreaker.State.OPEN -> {
                log.warn { "[PendingAnalysisScheduler] 서킷 OPEN 상태 - 외부 API 호출 skip" }
                return
            }

            CircuitBreaker.State.HALF_OPEN -> {
                runBlocking { commandService.processPendingAnalyses(limit = 1) }
            }

            else -> {
                runBlocking { commandService.processPendingAnalyses() }
            }
        }
        log.info { "[PendingAnalysisScheduler] finished" }
    }
}
