package com.mock.realteeth.ui.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus
import io.swagger.v3.oas.annotations.media.Schema

data class AnalyzeToothResponse(
    @field:Schema(description = "생성된 치아 분석 작업 ID", example = "1")
    val toothAnalysisId: Long,
    @field:Schema(description = "초기 분석 상태 (항상 PENDING으로 시작)", example = "PENDING")
    val toothAnalysisStatus: ToothAnalysisStatus,
)
