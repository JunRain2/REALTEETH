package com.mock.realteeth.infra.scheduler

import com.mock.realteeth.command.application.ToothAnalysisService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ProcessingAnalysisScheduler(
    private val commandService: ToothAnalysisService,
) {
    @Scheduled(fixedDelayString = "\${app.scheduler.processing-analysis.fixed-delay-ms:10000}")
    @SchedulerLock(name = "processingAnalysisScheduler", lockAtMostFor = "\${app.scheduler.processing-analysis.lock-at-most-for:PT30S}")
    fun processProcessing() {
        log.info { "[ProcessingAnalysisScheduler] started" }
        runBlocking { commandService.processProcessingAnalyses() }
        log.info { "[ProcessingAnalysisScheduler] finished" }
    }
}
