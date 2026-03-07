package com.mock.realteeth.query.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus

data class GetToothAnalysisResult(
    val toothAnalysisId: Long,
    val toothAnalysisStatus: ToothAnalysisStatus,
    val jobId: String?,
    val imageUrl: String,
    val result: String?,
)
