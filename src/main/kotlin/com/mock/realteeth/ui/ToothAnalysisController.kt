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

@Validated
@RestController
@RequestMapping("/api/v1/tooth-analyses")
class ToothAnalysisController(
    private val toothAnalysisService: ToothAnalysisService,
    private val toothAnalysisQueryService: ToothAnalysisQueryService,
) {
    @PostMapping
    suspend fun analyzeTooth(
        @RequestBody @Valid request: AnalyzeToothRequest,
    ): ApiResponse<AnalyzeToothResponse> {
        val result = toothAnalysisService.analyzeTooth(AnalyzeToothCommand(request.imageUrl))
        return ApiResponse.ok(AnalyzeToothResponse(result.toothAnalysisId, result.toothAnalysisStatus))
    }

    @GetMapping("/{id}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAnalysisStatus(
        @PathVariable id: Long,
    ): Flow<ApiResponse<StreamAnalysisStatusResult>> = toothAnalysisQueryService.streamAnalysisStatus(id).map { ApiResponse.ok(it) }

    @GetMapping("/{id}")
    suspend fun getToothAnalysis(
        @PathVariable id: Long,
    ): ApiResponse<GetToothAnalysisResult> = ApiResponse.ok(toothAnalysisQueryService.getToothAnalysis(id))

    @GetMapping
    suspend fun listToothAnalyses(
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) status: ToothAnalysisStatus?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ApiResponse<ListToothAnalysisResult> {
        val resolvedTo = to ?: LocalDate.now()
        val resolvedFrom = from ?: resolvedTo.minusMonths(6)
        return ApiResponse.ok(
            toothAnalysisQueryService.listToothAnalyses(ListToothAnalysisQuery(resolvedFrom, resolvedTo, status, page, size)),
        )
    }
}
