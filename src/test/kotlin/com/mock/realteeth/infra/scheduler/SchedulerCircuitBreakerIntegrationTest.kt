package com.mock.realteeth.infra.scheduler

import com.mock.realteeth.command.domain.FetchStatusResult
import com.mock.realteeth.command.domain.RequestAnalysisResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisRepository
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import com.mock.realteeth.command.domain.ToothImageRepository
import com.mock.realteeth.config.StubToothAnalysisClientConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.SimpleLock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource
import java.util.Optional
import kotlin.test.assertEquals

@SpringBootTest
@TestPropertySource(
    properties = [
        // 백그라운드 스케줄러가 직접 호출 테스트와 간섭하지 않도록 딜레이를 매우 길게 설정
        "app.scheduler.pending-analysis.fixed-delay-ms=3600000",
        "app.scheduler.processing-analysis.fixed-delay-ms=3600000",
    ],
)
@Import(SchedulerCircuitBreakerIntegrationTest.NoOpLockProviderConfig::class)
class SchedulerCircuitBreakerIntegrationTest(
    @Autowired private val pendingScheduler: PendingAnalysisScheduler,
    @Autowired private val processingScheduler: ProcessingAnalysisScheduler,
    @Autowired private val circuitBreaker: CircuitBreaker,
    @Autowired private val toothAnalysisRepository: ToothAnalysisRepository,
    @Autowired private val toothImageRepository: ToothImageRepository,
) {
    @TestConfiguration
    class NoOpLockProviderConfig {
        @Bean
        @Primary
        fun noOpLockProvider(): LockProvider =
            LockProvider { _: LockConfiguration ->
                Optional.of(SimpleLock { })
            }
    }

    @BeforeEach
    fun setUp() =
        runBlocking {
            StubToothAnalysisClientConfig.requestAnalysisResult =
                RequestAnalysisResult(ToothAnalysisStatus.PROCESSING, jobId = "stub-job-id")
            StubToothAnalysisClientConfig.fetchStatusResult =
                FetchStatusResult(ToothAnalysisStatus.PROCESSING)
            toothAnalysisRepository.deleteAll()
            toothImageRepository.deleteAll()
            circuitBreaker.reset()
        }

    @AfterEach
    fun tearDown() {
        circuitBreaker.reset()
    }

    // ===== PendingAnalysisScheduler =====

    @Test
    fun `PendingAnalysisScheduler - 서킷 CLOSED 상태에서 모든 PENDING 분석이 처리된다`() {
        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
        }

        pendingScheduler.processPending()

        val processing =
            runBlocking {
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PROCESSING)
                    .toList()
            }
        assertEquals(3, processing.size)
    }

    @Test
    fun `PendingAnalysisScheduler - 서킷 OPEN 상태에서는 아무것도 처리되지 않는다`() {
        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
        }

        circuitBreaker.transitionToOpenState()
        pendingScheduler.processPending()

        val pending =
            runBlocking {
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PENDING)
                    .toList()
            }
        assertEquals(3, pending.size)
    }

    @Test
    fun `PendingAnalysisScheduler - 서킷 HALF_OPEN 상태에서는 1건만 처리된다`() {
        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
        }

        circuitBreaker.transitionToOpenState()
        circuitBreaker.transitionToHalfOpenState()
        pendingScheduler.processPending()

        val processing =
            runBlocking {
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PROCESSING)
                    .toList()
            }
        assertEquals(1, processing.size)
    }

    // ===== ProcessingAnalysisScheduler =====

    @Test
    fun `ProcessingAnalysisScheduler - 서킷 CLOSED 상태에서 모든 PROCESSING 분석이 처리된다`() {
        StubToothAnalysisClientConfig.fetchStatusResult =
            FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "완료")

        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
                toothAnalysis.applyAnalysisResult(
                    RequestAnalysisResult(ToothAnalysisStatus.PROCESSING, jobId = "job-$i"),
                )
                toothAnalysisRepository.save(toothAnalysis)
            }
        }

        processingScheduler.processProcessing()

        val completed =
            runBlocking {
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.COMPLETED)
                    .toList()
            }
        assertEquals(3, completed.size)
    }

    @Test
    fun `ProcessingAnalysisScheduler - 서킷 OPEN 상태에서는 아무것도 처리되지 않는다`() {
        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
                toothAnalysis.applyAnalysisResult(
                    RequestAnalysisResult(ToothAnalysisStatus.PROCESSING, jobId = "job-$i"),
                )
                toothAnalysisRepository.save(toothAnalysis)
            }
        }

        circuitBreaker.transitionToOpenState()
        processingScheduler.processProcessing()

        val processing =
            runBlocking {
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PROCESSING)
                    .toList()
            }
        assertEquals(3, processing.size)
    }

    @Test
    fun `ProcessingAnalysisScheduler - 서킷 HALF_OPEN 상태에서는 1건만 처리된다`() {
        StubToothAnalysisClientConfig.fetchStatusResult =
            FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "완료")

        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
                toothAnalysis.applyAnalysisResult(
                    RequestAnalysisResult(ToothAnalysisStatus.PROCESSING, jobId = "job-$i"),
                )
                toothAnalysisRepository.save(toothAnalysis)
            }
        }

        circuitBreaker.transitionToOpenState()
        circuitBreaker.transitionToHalfOpenState()
        processingScheduler.processProcessing()

        val completed =
            runBlocking {
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.COMPLETED)
                    .toList()
            }
        assertEquals(1, completed.size)
    }
}
