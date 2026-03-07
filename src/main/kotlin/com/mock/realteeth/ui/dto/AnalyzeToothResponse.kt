package com.mock.realteeth.ui.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus

data class AnalyzeToothResponse(
    val toothAnalysisId: Long,
    val toothAnalysisStatus: ToothAnalysisStatus,
)
