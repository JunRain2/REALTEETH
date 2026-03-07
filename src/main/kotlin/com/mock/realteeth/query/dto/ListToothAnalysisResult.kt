package com.mock.realteeth.query.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus
import java.time.LocalDateTime

data class ListToothAnalysisResult(
    val items: List<ListToothAnalysisItem>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
)

data class ListToothAnalysisItem(
    val id: Long,
    val status: ToothAnalysisStatus,
    val imageUrl: String,
    val result: String?,
    val createdAt: LocalDateTime?,
)
