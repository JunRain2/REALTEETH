package com.mock.realteeth.command.application

import com.mock.realteeth.command.application.dto.AnalyzeToothCommand
import com.mock.realteeth.command.domain.FetchStatusResult
import com.mock.realteeth.command.domain.RequestAnalysisResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisRepository
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import com.mock.realteeth.command.domain.ToothImageRepository
import com.mock.realteeth.config.StubToothAnalysisClientConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class ToothAnalysisServiceIntegrationTest(
    @Autowired private val commandService: ToothAnalysisService,
    @Autowired private val toothImageRepository: ToothImageRepository,
    @Autowired private val toothAnalysisRepository: ToothAnalysisRepository,
) {
    @BeforeEach
    fun setUp() =
        runBlocking {
            StubToothAnalysisClientConfig.requestAnalysisResult =
                RequestAnalysisResult(ToothAnalysisStatus.PROCESSING, jobId = "stub-job-id")
            StubToothAnalysisClientConfig.fetchStatusResult =
                FetchStatusResult(ToothAnalysisStatus.PROCESSING)
            toothAnalysisRepository.deleteAll()
            toothImageRepository.deleteAll()
        }

    // ===== analyzeTooth =====

    @Test
    fun `analyzeTooth - 결과를 반환한다`() =
        runTest {
            val result = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))

            assertTrue(result.toothAnalysisId > 0)
            assertEquals(ToothAnalysisStatus.PENDING, result.toothAnalysisStatus)
        }

    @Test
    fun `analyzeTooth - ToothImage와 ToothAnalysis가 DB에 저장된다`() =
        runTest {
            val imageUrl = "https://example.com/tooth.jpg"
            val result = commandService.analyzeTooth(AnalyzeToothCommand(imageUrl))

            val toothAnalysis = assertNotNull(toothAnalysisRepository.findById(result.toothAnalysisId))
            assertEquals(ToothAnalysisStatus.PENDING, toothAnalysis.status)
            assertNotEquals(0, toothAnalysis.id)

            val toothImage = assertNotNull(toothImageRepository.findById(toothAnalysis.imageId))
            assertNotEquals(0, toothImage.id)
            assertEquals(imageUrl, toothImage.url)
        }

    // ===== processPendingAnalyses =====

    @Test
    fun `processPendingAnalyses - PENDING 분석을 PROCESSING으로 전환하고 jobId와 sendCount를 저장한다`() =
        runTest {
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))

            commandService.processPendingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(created.toothAnalysisId))
            assertEquals(ToothAnalysisStatus.PROCESSING, saved.status)
            assertEquals("stub-job-id", saved.jobId)
            assertEquals(1, saved.sendCount)
        }

    @Test
    fun `processPendingAnalyses - sendCount가 maxSendCount 이상이면 FAILED로 마킹된다`() =
        runTest {
            val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth.jpg"))
            val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))

            // sendCount를 maxSendCount(5)까지 올리되 PENDING 상태 유지
            val pendingResult = RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.PENDING, jobId = null)
            repeat(5) { toothAnalysis.applyAnalysisResult(pendingResult) }
            toothAnalysisRepository.save(toothAnalysis)

            commandService.processPendingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(toothAnalysis.id))
            assertEquals(ToothAnalysisStatus.FAILED, saved.status)
            assertNotNull(saved.result)
        }

    @Test
    fun `processPendingAnalyses - PENDING이 없으면 아무것도 하지 않는다`() =
        runTest {
            commandService.processPendingAnalyses()

            assertEquals(0L, toothAnalysisRepository.count())
        }

    @Test
    fun `processPendingAnalyses - requestAnalysis가 FAILED 반환 시 FAILED로 마킹된다`() =
        runTest {
            StubToothAnalysisClientConfig.requestAnalysisResult =
                RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.FAILED, result = "분석 실패")
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))

            commandService.processPendingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(created.toothAnalysisId))
            assertEquals(ToothAnalysisStatus.FAILED, saved.status)
            assertEquals("분석 실패", saved.result)
        }

    @Test
    fun `processPendingAnalyses - 여러 건의 PENDING이 있을 때 모두 처리된다`() =
        runTest {
            repeat(3) { i ->
                commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth$i.jpg"))
            }

            commandService.processPendingAnalyses()

            val processed =
                toothAnalysisRepository
                    .findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PROCESSING)
                    .toList()
            assertEquals(3, processed.size)
        }

    // ===== processProcessingAnalyses =====

    @Test
    fun `processProcessingAnalyses - PROCESSING 분석의 상태를 fetchStatus 결과로 업데이트한다`() =
        runTest {
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))
            commandService.processPendingAnalyses()

            commandService.processProcessingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(created.toothAnalysisId))
            assertEquals(ToothAnalysisStatus.PROCESSING, saved.status)
        }

    @Test
    fun `processProcessingAnalyses - fetchStatus가 COMPLETED 반환 시 COMPLETED와 result가 저장된다`() =
        runTest {
            StubToothAnalysisClientConfig.fetchStatusResult =
                FetchStatusResult(analysisStatus = ToothAnalysisStatus.COMPLETED, result = "분석 완료 결과")
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))
            commandService.processPendingAnalyses()

            commandService.processProcessingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(created.toothAnalysisId))
            assertEquals(ToothAnalysisStatus.COMPLETED, saved.status)
            assertEquals("분석 완료 결과", saved.result)
        }

    @Test
    fun `processProcessingAnalyses - fetchStatus가 FAILED 반환 시 FAILED와 cause가 저장된다`() =
        runTest {
            StubToothAnalysisClientConfig.fetchStatusResult =
                FetchStatusResult(analysisStatus = ToothAnalysisStatus.FAILED, result = "처리 실패")
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))
            commandService.processPendingAnalyses()

            commandService.processProcessingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(created.toothAnalysisId))
            assertEquals(ToothAnalysisStatus.FAILED, saved.status)
            assertEquals("처리 실패", saved.result)
        }

    @Test
    fun `processProcessingAnalyses - sendCount가 maxSendCount 이상이면 FAILED로 마킹된다`() =
        runTest {
            val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth.jpg"))
            val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))

            // sendCount를 maxSendCount(5)까지 올리고 PROCESSING 상태로 설정
            val processingResult = RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.PROCESSING, jobId = "job")
            repeat(5) { toothAnalysis.applyAnalysisResult(processingResult) }
            toothAnalysisRepository.save(toothAnalysis)

            commandService.processProcessingAnalyses()

            val saved = assertNotNull(toothAnalysisRepository.findById(toothAnalysis.id))
            assertEquals(ToothAnalysisStatus.FAILED, saved.status)
            assertNotNull(saved.result)
        }

    @Test
    fun `processProcessingAnalyses - PROCESSING이 없으면 아무것도 하지 않는다`() =
        runTest {
            commandService.processProcessingAnalyses()

            assertEquals(0L, toothAnalysisRepository.count())
        }
}
