package com.mock.realteeth.command.application.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus

data class AnalyzeToothResult(
    val toothAnalysisId: Long,
    val toothAnalysisStatus: ToothAnalysisStatus,
)
