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
class ProcessingAnalysisScheduler(
    private val commandService: ToothAnalysisService,
    private val circuitBreaker: CircuitBreaker,
) {
    @Scheduled(fixedDelayString = "\${app.scheduler.processing-analysis.fixed-delay-ms:10000}")
    @SchedulerLock(name = "processingAnalysisScheduler", lockAtMostFor = "\${app.scheduler.processing-analysis.lock-at-most-for:PT30S}")
    fun processProcessing() {
        log.info { "[ProcessingAnalysisScheduler] started" }
        when (circuitBreaker.state) {
            CircuitBreaker.State.OPEN -> {
                log.warn { "[ProcessingAnalysisScheduler] 서킷 OPEN 상태 - 외부 API 호출 skip" }
                return
            }
            CircuitBreaker.State.HALF_OPEN ->
                runBlocking { commandService.processProcessingAnalyses(limit = 1) }
            else ->
                runBlocking { commandService.processProcessingAnalyses() }
        }
        log.info { "[ProcessingAnalysisScheduler] finished" }
    }
}
