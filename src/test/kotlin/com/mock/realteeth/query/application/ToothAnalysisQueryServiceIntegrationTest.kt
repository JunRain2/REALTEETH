package com.mock.realteeth.query.application

import com.mock.realteeth.command.application.ToothAnalysisService
import com.mock.realteeth.command.application.dto.AnalyzeToothCommand
import com.mock.realteeth.command.domain.FetchStatusResult
import com.mock.realteeth.command.domain.RequestAnalysisResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisRepository
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import com.mock.realteeth.command.domain.ToothImageRepository
import com.mock.realteeth.command.domain.exception.BusinessException
import com.mock.realteeth.command.domain.exception.ErrorCode
import com.mock.realteeth.config.StubToothAnalysisClientConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
class ToothAnalysisQueryServiceIntegrationTest(
    @Autowired private val queryService: ToothAnalysisQueryService,
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

    // ===== streamAnalysisStatus =====

    @Test
    fun `streamAnalysisStatus - 존재하지 않는 ID면 TOOTH_ANALYSIS_NOT_FOUND 예외를 던진다`() =
        runTest {
            val exception =
                assertFailsWith<BusinessException> {
                    queryService.streamAnalysisStatus(999L).first()
                }

            assertEquals(ErrorCode.TOOTH_ANALYSIS_NOT_FOUND, exception.errorCode)
        }

    @Test
    fun `streamAnalysisStatus - COMPLETED 상태 분석은 단건 emit 후 종료된다`() =
        runTest {
            val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth.jpg"))
            val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            toothAnalysis.applyFetchStatusResult(FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "완료"))
            toothAnalysisRepository.save(toothAnalysis)

            val results = queryService.streamAnalysisStatus(toothAnalysis.id).toList()

            assertEquals(1, results.size)
            assertEquals(ToothAnalysisStatus.COMPLETED, results[0].toothAnalysisStatus)
        }

    @Test
    fun `streamAnalysisStatus - PENDING 상태 분석은 현재 상태를 emit한다`() =
        runTest {
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg"))

            val firstResult = queryService.streamAnalysisStatus(created.toothAnalysisId).first()

            assertEquals(ToothAnalysisStatus.PENDING, firstResult.toothAnalysisStatus)
            assertEquals(created.toothAnalysisId, firstResult.toothAnalysisId)
        }
}
