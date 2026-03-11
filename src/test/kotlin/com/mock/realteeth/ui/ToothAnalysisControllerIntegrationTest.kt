package com.mock.realteeth.ui

import com.mock.realteeth.command.domain.FetchStatusResult
import com.mock.realteeth.command.domain.RequestAnalysisResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisRepository
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import com.mock.realteeth.command.domain.ToothImageRepository
import com.mock.realteeth.config.StubToothAnalysisClientConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToothAnalysisControllerIntegrationTest(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val toothAnalysisRepository: ToothAnalysisRepository,
    @Autowired private val toothImageRepository: ToothImageRepository,
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

    // ===== POST /api/v1/tooth-analyses =====

    @Test
    fun `POST analyzeTooth - 유효한 imageUrl로 요청 시 200과 PENDING 상태를 반환한다`() {
        webTestClient
            .post()
            .uri("/api/v1/tooth-analyses")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"imageUrl": "https://example.com/tooth.jpg", "imageHash": "d41d8cd98f00b204e9800998ecf8427e"}""")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(true)
            .jsonPath("$.data.toothAnalysisId")
            .isNumber
            .jsonPath("$.data.toothAnalysisStatus")
            .isEqualTo("PENDING")
            .jsonPath("$.error")
            .isEmpty
    }

    @Test
    fun `POST analyzeTooth - 동일 imageHash로 재요청 시 분석이 중복 생성되지 않는다`() {
        val body = """{"imageUrl": "https://example.com/tooth.jpg", "imageHash": "d41d8cd98f00b204e9800998ecf8427e"}"""

        webTestClient
            .post()
            .uri("/api/v1/tooth-analyses")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

        webTestClient
            .post()
            .uri("/api/v1/tooth-analyses")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

        assert(runBlocking { toothAnalysisRepository.count() } == 1L) {
            "동일 해시 재요청 시 분석이 1건만 존재해야 한다"
        }
    }

    @Test
    fun `POST analyzeTooth - imageUrl이 빈 문자열이면 400과 validation 에러를 반환한다`() {
        webTestClient
            .post()
            .uri("/api/v1/tooth-analyses")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"imageUrl": "", "imageHash": "d41d8cd98f00b204e9800998ecf8427e"}""")
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.data")
            .isEmpty
            .jsonPath("$.error.message")
            .isEqualTo("입력값이 올바르지 않습니다")
            .jsonPath("$.error.details[0]")
            .value<String> { detail ->
                assert(detail.startsWith("imageUrl:")) { "details should contain imageUrl error, got: $detail" }
            }
    }

    @Test
    fun `POST analyzeTooth - imageUrl이 null이면 에러를 반환한다`() {
        // null은 non-nullable String 필드 역직렬화 실패로 DecodingException → catch-all → 500
        webTestClient
            .post()
            .uri("/api/v1/tooth-analyses")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"imageUrl": null, "imageHash": "d41d8cd98f00b204e9800998ecf8427e"}""")
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
    }

    @Test
    fun `POST analyzeTooth - 요청 본문이 없으면 에러를 반환한다`() {
        // 빈 본문은 DecodingException → catch-all → 500
        webTestClient
            .post()
            .uri("/api/v1/tooth-analyses")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
    }

    // ===== GET /api/v1/tooth-analyses/{id} =====

    @Test
    fun `GET getToothAnalysis - 존재하는 ID로 조회 시 200과 상세 정보를 반환한다`() {
        val imageUrl = "https://example.com/tooth.jpg"
        val toothImage = runBlocking { toothImageRepository.save(ToothImage(imageUrl)) }
        val toothAnalysis = runBlocking { toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage)) }

        webTestClient
            .get()
            .uri("/api/v1/tooth-analyses/{id}", toothAnalysis.id)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(true)
            .jsonPath("$.data.toothAnalysisId")
            .isEqualTo(toothAnalysis.id.toInt())
            .jsonPath("$.data.toothAnalysisStatus")
            .isEqualTo("PENDING")
            .jsonPath("$.data.imageUrl")
            .isEqualTo(imageUrl)
            .jsonPath("$.data.jobId")
            .isEmpty
            .jsonPath("$.data.result")
            .isEmpty
    }

    @Test
    fun `GET getToothAnalysis - 존재하지 않는 ID 조회 시 404와 TOOTH_ANALYSIS_NOT_FOUND를 반환한다`() {
        webTestClient
            .get()
            .uri("/api/v1/tooth-analyses/{id}", 999L)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.code")
            .isEqualTo("TOOTH_ANALYSIS_NOT_FOUND")
    }

    // ===== GET /api/v1/tooth-analyses =====

    @Test
    fun `GET listToothAnalyses - 파라미터 없이 호출 시 200과 기본 페이지를 반환한다`() {
        runBlocking {
            repeat(3) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
        }

        webTestClient
            .get()
            .uri("/api/v1/tooth-analyses")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(true)
            .jsonPath("$.data.totalCount")
            .isEqualTo(3)
            .jsonPath("$.data.page")
            .isEqualTo(0)
            .jsonPath("$.data.size")
            .isEqualTo(20)
            .jsonPath("$.data.items.length()")
            .isEqualTo(3)
    }

    @Test
    fun `GET listToothAnalyses - status 필터로 조회 시 해당 상태의 항목만 반환한다`() {
        runBlocking {
            repeat(2) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/pending$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
            val completedImage = toothImageRepository.save(ToothImage("https://example.com/completed.jpg"))
            val completedAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(completedImage))
            completedAnalysis.applyFetchStatusResult(FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "결과"))
            toothAnalysisRepository.save(completedAnalysis)
        }

        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("status", "PENDING")
                    .build()
            }.exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.totalCount")
            .isEqualTo(2)
            .jsonPath("$.data.items.length()")
            .isEqualTo(2)
    }

    @Test
    fun `GET listToothAnalyses - page와 size로 페이지네이션이 동작한다`() {
        runBlocking {
            repeat(5) { i ->
                val toothImage = toothImageRepository.save(ToothImage("https://example.com/tooth$i.jpg"))
                toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))
            }
        }

        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("page", "0")
                    .queryParam("size", "3")
                    .build()
            }.exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.items.length()")
            .isEqualTo(3)
            .jsonPath("$.data.totalCount")
            .isEqualTo(5)
            .jsonPath("$.data.page")
            .isEqualTo(0)

        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("page", "1")
                    .queryParam("size", "3")
                    .build()
            }.exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.items.length()")
            .isEqualTo(2)
            .jsonPath("$.data.totalCount")
            .isEqualTo(5)
            .jsonPath("$.data.page")
            .isEqualTo(1)
    }

    @Test
    fun `GET listToothAnalyses - from이 to보다 크면 400과 INVALID_INPUT을 반환한다`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("from", "2025-06-01")
                    .queryParam("to", "2025-01-01")
                    .build()
            }.exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.code")
            .isEqualTo("INVALID_INPUT")
    }

    @Test
    fun `GET listToothAnalyses - 날짜 범위가 6개월 초과면 400과 DATE_RANGE_TOO_LARGE를 반환한다`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("from", "2024-01-01")
                    .queryParam("to", "2024-07-02")
                    .build()
            }.exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.code")
            .isEqualTo("DATE_RANGE_TOO_LARGE")
    }

    @Test
    fun `GET listToothAnalyses - page가 음수면 400과 validation 에러를 반환한다`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("page", "-1")
                    .build()
            }.exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.message")
            .isEqualTo("입력값이 올바르지 않습니다")
            .jsonPath("$.error.details")
            .isNotEmpty
    }

    @Test
    fun `GET listToothAnalyses - size가 0이면 400과 validation 에러를 반환한다`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("size", "0")
                    .build()
            }.exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.message")
            .isEqualTo("입력값이 올바르지 않습니다")
    }

    @Test
    fun `GET listToothAnalyses - size가 101이면 400과 validation 에러를 반환한다`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/v1/tooth-analyses")
                    .queryParam("size", "101")
                    .build()
            }.exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.message")
            .isEqualTo("입력값이 올바르지 않습니다")
    }

    // ===== GET /api/v1/tooth-analyses/{id}/stream =====

    @Test
    fun `GET streamAnalysisStatus - COMPLETED 분석은 단건 SSE 이벤트를 emit하고 스트림이 종료된다`() {
        val toothImage = runBlocking { toothImageRepository.save(ToothImage("https://example.com/tooth.jpg")) }
        val toothAnalysis = runBlocking { toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage)) }
        runBlocking {
            toothAnalysis.applyFetchStatusResult(FetchStatusResult(ToothAnalysisStatus.COMPLETED, result = "완료"))
            toothAnalysisRepository.save(toothAnalysis)
        }

        val responseBody =
            webTestClient
                .get()
                .uri("/api/v1/tooth-analyses/{id}/stream", toothAnalysis.id)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult(String::class.java)
                .responseBody

        StepVerifier
            .create(responseBody)
            .expectNextMatches { it.contains("COMPLETED") }
            .verifyComplete()
    }
}
