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
import com.mock.realteeth.query.dto.ListToothAnalysisQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            val created = commandService.analyzeTooth(AnalyzeToothCommand("https://example.com/tooth.jpg", "abc123hash001"))

            val firstResult = queryService.streamAnalysisStatus(created.toothAnalysisId).first()

            assertEquals(ToothAnalysisStatus.PENDING, firstResult.toothAnalysisStatus)
            assertEquals(created.toothAnalysisId, firstResult.toothAnalysisId)
        }

    // ===== getToothAnalysis =====

    @Test
    fun `getToothAnalysis - 존재하는 ID로 조회 시 모든 필드가 정확히 반환된다`() =
        runTest {
            val imageUrl = "https://example.com/tooth.jpg"
            val toothImage = toothImageRepository.save(ToothImage(imageUrl))
            val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))

            val result = queryService.getToothAnalysis(toothAnalysis.id)

            assertEquals(toothAnalysis.id, result.toothAnalysisId)
            assertEquals(ToothAnalysisStatus.PENDING, result.toothAnalysisStatus)
            assertEquals(imageUrl, result.imageUrl)
            assertNull(result.jobId)
            assertNull(result.result)
        }

    @Test
    fun `getToothAnalysis - 존재하지 않는 ID 조회 시 TOOTH_ANALYSIS_NOT_FOUND 예외를 던진다`() =
        runTest {
            val exception =
                assertFailsWith<BusinessException> {
                    queryService.getToothAnalysis(999L)
                }

            assertEquals(ErrorCode.TOOTH_ANALYSIS_NOT_FOUND, exception.errorCode)
        }

    @Test
    fun `getToothAnalysis - COMPLETED 상태 분석은 result 필드가 포함된다`() =
        runTest {
            val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth.jpg"))
            val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            toothAnalysis.applyFetchStatusResult(FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "완료 결과"))
            toothAnalysisRepository.save(toothAnalysis)

            val result = queryService.getToothAnalysis(toothAnalysis.id)

            assertEquals(ToothAnalysisStatus.COMPLETED, result.toothAnalysisStatus)
            assertEquals("완료 결과", result.result)
        }

    @Test
    fun `getToothAnalysis - soft-delete된 분석은 조회되지 않는다`() =
        runTest {
            val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth.jpg"))
            val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            toothAnalysis.delete()
            toothAnalysisRepository.save(toothAnalysis)

            val exception =
                assertFailsWith<BusinessException> {
                    queryService.getToothAnalysis(toothAnalysis.id)
                }

            assertEquals(ErrorCode.TOOTH_ANALYSIS_NOT_FOUND, exception.errorCode)
        }

    // ===== listToothAnalyses =====

    @Test
    fun `listToothAnalyses - from이 to보다 크면 INVALID_INPUT 예외를 던진다`() =
        runTest {
            val exception =
                assertFailsWith<BusinessException> {
                    queryService.listToothAnalyses(
                        ListToothAnalysisQuery(
                            from = LocalDate.of(2025, 6, 1),
                            to = LocalDate.of(2025, 1, 1),
                            status = null,
                            page = 0,
                            size = 20,
                        ),
                    )
                }

            assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
        }

    @Test
    fun `listToothAnalyses - 날짜 범위가 정확히 6개월이면 예외 없이 정상 반환된다`() =
        runTest {
            val result =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.of(2024, 1, 1),
                        to = LocalDate.of(2024, 7, 1),
                        status = null,
                        page = 0,
                        size = 20,
                    ),
                )

            assertNotNull(result)
        }

    @Test
    fun `listToothAnalyses - 날짜 범위가 6개월 1일 초과면 DATE_RANGE_TOO_LARGE 예외를 던진다`() =
        runTest {
            val exception =
                assertFailsWith<BusinessException> {
                    queryService.listToothAnalyses(
                        ListToothAnalysisQuery(
                            from = LocalDate.of(2024, 1, 1),
                            to = LocalDate.of(2024, 7, 2),
                            status = null,
                            page = 0,
                            size = 20,
                        ),
                    )
                }

            assertEquals(ErrorCode.DATE_RANGE_TOO_LARGE, exception.errorCode)
        }

    @Test
    fun `listToothAnalyses - 데이터가 없으면 빈 목록과 totalCount 0을 반환한다`() =
        runTest {
            val result =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.now().minusMonths(1),
                        to = LocalDate.now(),
                        status = null,
                        page = 0,
                        size = 20,
                    ),
                )

            assertTrue(result.items.isEmpty())
            assertEquals(0L, result.totalCount)
        }

    @Test
    fun `listToothAnalyses - status 필터 적용 시 해당 상태의 항목만 반환된다`() =
        runTest {
            repeat(2) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/pending$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
            val completedImage = toothImageRepository.save(ToothImage("https://example.com/completed.jpg"))
            val completedAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(completedImage))
            completedAnalysis.applyFetchStatusResult(FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "결과"))
            toothAnalysisRepository.save(completedAnalysis)

            val pendingResult =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.now().minusMonths(1),
                        to = LocalDate.now(),
                        status = ToothAnalysisStatus.PENDING,
                        page = 0,
                        size = 20,
                    ),
                )
            assertEquals(2, pendingResult.items.size)
            assertEquals(2L, pendingResult.totalCount)
            assertTrue(pendingResult.items.all { it.status == ToothAnalysisStatus.PENDING })

            val completedResult =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.now().minusMonths(1),
                        to = LocalDate.now(),
                        status = ToothAnalysisStatus.COMPLETED,
                        page = 0,
                        size = 20,
                    ),
                )
            assertEquals(1, completedResult.items.size)
            assertEquals(1L, completedResult.totalCount)
        }

    @Test
    fun `listToothAnalyses - 페이지네이션이 올바르게 동작한다`() =
        runTest {
            repeat(5) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }

            val firstPage =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.now().minusMonths(1),
                        to = LocalDate.now(),
                        status = null,
                        page = 0,
                        size = 3,
                    ),
                )
            assertEquals(3, firstPage.items.size)
            assertEquals(5L, firstPage.totalCount)

            val secondPage =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.now().minusMonths(1),
                        to = LocalDate.now(),
                        status = null,
                        page = 1,
                        size = 3,
                    ),
                )
            assertEquals(2, secondPage.items.size)
            assertEquals(5L, secondPage.totalCount)

            val emptyPage =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = LocalDate.now().minusMonths(1),
                        to = LocalDate.now(),
                        status = null,
                        page = 2,
                        size = 3,
                    ),
                )
            assertTrue(emptyPage.items.isEmpty())
            assertEquals(5L, emptyPage.totalCount)
        }

    @Test
    fun `listToothAnalyses - 날짜 범위 밖 데이터는 조회되지 않는다`() =
        runTest {
            val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth.jpg"))
            toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))

            val tomorrow = LocalDate.now().plusDays(1)
            val result =
                queryService.listToothAnalyses(
                    ListToothAnalysisQuery(
                        from = tomorrow,
                        to = tomorrow.plusMonths(1),
                        status = null,
                        page = 0,
                        size = 20,
                    ),
                )

            assertTrue(result.items.isEmpty())
            assertEquals(0L, result.totalCount)
        }
}
