package com.mock.realteeth.query.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ListToothAnalysisResult(
    @field:Schema(description = "조회된 분석 목록")
    val items: List<ListToothAnalysisItem>,
    @field:Schema(description = "전체 데이터 수", example = "42")
    val totalCount: Long,
    @field:Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val page: Int,
    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,
)

data class ListToothAnalysisItem(
    @field:Schema(description = "치아 분석 작업 ID", example = "1")
    val id: Long,
    @field:Schema(description = "현재 분석 상태", example = "COMPLETED")
    val status: ToothAnalysisStatus,
    @field:Schema(description = "분석 대상 이미지 URL", example = "https://example.com/tooth.jpg")
    val imageUrl: String,
    @field:Schema(description = "분석 결과 또는 실패 원인", example = "정상", nullable = true)
    val result: String?,
    @field:Schema(description = "작업 생성 일시", example = "2026-03-11T10:00:00")
    val createdAt: LocalDateTime?,
)
