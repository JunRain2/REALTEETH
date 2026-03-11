package com.mock.realteeth.ui

import com.mock.realteeth.command.application.ToothAnalysisService
import com.mock.realteeth.command.application.dto.AnalyzeToothCommand
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.query.application.ToothAnalysisQueryService
import com.mock.realteeth.query.dto.GetToothAnalysisResult
import com.mock.realteeth.query.dto.ListToothAnalysisQuery
import com.mock.realteeth.query.dto.ListToothAnalysisResult
import com.mock.realteeth.query.dto.StreamAnalysisStatusResult
import com.mock.realteeth.ui.dto.AnalyzeToothRequest
import com.mock.realteeth.ui.dto.AnalyzeToothResponse
import com.mock.realteeth.ui.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "치아 분석", description = "치아 이미지 AI 분석 요청 및 결과 조회 API")
@Validated
@RestController
@RequestMapping("/api/v1/tooth-analyses")
class ToothAnalysisController(
    private val toothAnalysisService: ToothAnalysisService,
    private val toothAnalysisQueryService: ToothAnalysisQueryService,
) {
    @Operation(
        summary = "치아 분석 요청",
        description = "이미지 URL을 전달하면 비동기로 AI 분석을 요청합니다. 응답으로 작업 ID와 초기 상태(PENDING)를 반환합니다.",
    )
    @PostMapping
    suspend fun analyzeTooth(
        @RequestBody @Valid request: AnalyzeToothRequest,
    ): ApiResponse<AnalyzeToothResponse> {
        val result = toothAnalysisService.analyzeTooth(AnalyzeToothCommand(request.imageUrl, request.imageHash))
        return ApiResponse.ok(AnalyzeToothResponse(result.toothAnalysisId, result.toothAnalysisStatus))
    }

    @Operation(
        summary = "분석 상태 실시간 스트리밍 (SSE)",
        description = "Server-Sent Events로 분석 상태 변경을 실시간으로 수신합니다. 상태가 COMPLETED 또는 FAILED가 되면 스트림이 종료됩니다.",
    )
    @GetMapping("/{id}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAnalysisStatus(
        @Parameter(description = "치아 분석 작업 ID") @PathVariable id: Long,
    ): Flow<ApiResponse<StreamAnalysisStatusResult>> = toothAnalysisQueryService.streamAnalysisStatus(id).map { ApiResponse.ok(it) }

    @Operation(
        summary = "치아 분석 단건 조회",
        description = "분석 작업 ID로 현재 상태와 결과를 조회합니다.",
    )
    @GetMapping("/{id}")
    suspend fun getToothAnalysis(
        @Parameter(description = "치아 분석 작업 ID") @PathVariable id: Long,
    ): ApiResponse<GetToothAnalysisResult> = ApiResponse.ok(toothAnalysisQueryService.getToothAnalysis(id))

    @Operation(
        summary = "치아 분석 목록 조회",
        description = "생성일 기준으로 치아 분석 목록을 페이지네이션하여 조회합니다. 날짜 범위는 최대 6개월까지 지정할 수 있습니다.",
    )
    @GetMapping
    suspend fun listToothAnalyses(
        @Parameter(description = "조회 시작일 (기본값: to 기준 6개월 전, yyyy-MM-dd)") @RequestParam(required = false) from: LocalDate?,
        @Parameter(description = "조회 종료일 (기본값: 오늘, yyyy-MM-dd)") @RequestParam(required = false) to: LocalDate?,
        @Parameter(description = "상태 필터 (PENDING / PROCESSING / COMPLETED / FAILED)") @RequestParam(required = false) status:
            ToothAnalysisStatus?,
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ApiResponse<ListToothAnalysisResult> {
        val resolvedTo = to ?: LocalDate.now()
        val resolvedFrom = from ?: resolvedTo.minusMonths(6)
        return ApiResponse.ok(
            toothAnalysisQueryService.listToothAnalyses(ListToothAnalysisQuery(resolvedFrom, resolvedTo, status, page, size)),
        )
    }
}
