package com.mock.realteeth.query.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus
import io.swagger.v3.oas.annotations.media.Schema

data class StreamAnalysisStatusResult(
    @field:Schema(description = "치아 분석 작업 ID", example = "1")
    val toothAnalysisId: Long,
    @field:Schema(description = "현재 분석 상태", example = "PROCESSING")
    val toothAnalysisStatus: ToothAnalysisStatus,
    @field:Schema(description = "Mock Worker에서 발급된 Job ID", example = "job-abc123", nullable = true)
    val jobId: String?,
    @field:Schema(description = "분석 대상 이미지 URL", example = "https://example.com/tooth.jpg")
    val imageUrl: String,
    @field:Schema(description = "분석 결과 또는 실패 원인 (완료/실패 시 존재)", nullable = true)
    val result: String?,
)
