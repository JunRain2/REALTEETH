package com.mock.realteeth.infra.client

import com.mock.realteeth.command.domain.FetchStatusResult
import com.mock.realteeth.command.domain.RequestAnalysisResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisClient
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

private val log = KotlinLogging.logger {}

@Component
class ToothAnalysisClientImpl(
    private val toothAnalysisWebClient: WebClient,
    private val circuitBreaker: CircuitBreaker,
) : ToothAnalysisClient {
    override suspend fun requestAnalysis(toothImage: ToothImage): RequestAnalysisResult =
        try {
            val response =
                circuitBreaker.executeSuspendFunction {
                    toothAnalysisWebClient
                        .post()
                        .uri("/process")
                        .bodyValue(MockAnalyzeRequest(toothImage.url))
                        .retrieve()
                        .bodyToMono(MockAnalyzeResponse::class.java)
                        .awaitSingle()
                }

            RequestAnalysisResult(
                analysisStatus = response.status.toToothAnalysisStatus(),
                jobId = response.jobId,
            )
        } catch (e: CallNotPermittedException) {
            log.warn { "requestAnalysis circuit breaker open: imageUrl=${toothImage.url}" }
            RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.PENDING)
        } catch (e: WebClientResponseException) {
            when (val status = e.statusCode.value()) {
                400 -> {
                    val result = runCatching { e.getResponseBodyAs(MockErrorResponse::class.java)?.detail }.getOrNull()
                    log.warn(e) { "requestAnalysis bad request (FAILED): imageUrl=${toothImage.url}, detail=$result" }
                    RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.FAILED, result = result)
                }

                422 -> {
                    val result =
                        runCatching {
                            e.getResponseBodyAs(MockValidationErrorResponse::class.java)?.detail?.toString()
                        }.getOrNull()
                    log.warn(e) { "requestAnalysis validation error (FAILED): imageUrl=${toothImage.url}, detail=$result" }
                    RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.FAILED, result = result)
                }

                else -> {
                    log.warn(e) { "requestAnalysis retryable HTTP error: status=$status, imageUrl=${toothImage.url}" }
                    RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.PENDING)
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "requestAnalysis retryable unexpected error: imageUrl=${toothImage.url}" }
            RequestAnalysisResult(analysisStatus = ToothAnalysisStatus.PENDING)
        }

    override suspend fun fetchStatus(toothAnalysis: ToothAnalysis): FetchStatusResult =
        try {
            val response =
                circuitBreaker.executeSuspendFunction {
                    toothAnalysisWebClient
                        .get()
                        .uri("/process/${toothAnalysis.jobId}")
                        .retrieve()
                        .bodyToMono(MockStatusResponse::class.java)
                        .awaitSingle()
                }

            FetchStatusResult(analysisStatus = response.status.toToothAnalysisStatus(), result = response.result)
        } catch (e: CallNotPermittedException) {
            log.warn { "fetchStatus circuit breaker open: jobId=${toothAnalysis.jobId}" }
            FetchStatusResult(analysisStatus = ToothAnalysisStatus.PROCESSING)
        } catch (e: WebClientResponseException) {
            val status = e.statusCode.value()
            when (status) {
                404 -> {
                    log.warn(e) { "fetchStatus job not found, will retry: jobId=${toothAnalysis.jobId}" }
                    FetchStatusResult(analysisStatus = ToothAnalysisStatus.PENDING)
                }

                422 -> {
                    log.warn(e) { "fetchStatus validation error, will retry: jobId=${toothAnalysis.jobId}" }
                    FetchStatusResult(analysisStatus = ToothAnalysisStatus.PENDING)
                }

                else -> {
                    log.warn(e) { "fetchStatus retryable HTTP error: status=$status, jobId=${toothAnalysis.jobId}" }
                    FetchStatusResult(analysisStatus = ToothAnalysisStatus.PROCESSING)
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "fetchStatus retryable unexpected error: jobId=${toothAnalysis.jobId}" }
            FetchStatusResult(analysisStatus = ToothAnalysisStatus.PROCESSING)
        }

    private fun String.toToothAnalysisStatus(): ToothAnalysisStatus =
        when (this) {
            "PROCESSING" -> ToothAnalysisStatus.PROCESSING
            "COMPLETED" -> ToothAnalysisStatus.COMPLETED
            "FAILED" -> ToothAnalysisStatus.FAILED
            else -> ToothAnalysisStatus.FAILED
        }
}

private data class MockAnalyzeRequest(
    val imageUrl: String,
)

private data class MockAnalyzeResponse(
    val jobId: String,
    val status: String,
)

private data class MockStatusResponse(
    val jobId: String,
    val status: String,
    val result: String?,
)

private data class MockValidationErrorResponse(
    val detail: List<Detail>,
) {
    data class Detail(
        val loc: List<Any>,
        val msg: String,
        val type: String,
        val input: String? = null,
        val ctx: Map<String, Any>? = null,
    )
}

private data class MockErrorResponse(
    val detail: String,
)
