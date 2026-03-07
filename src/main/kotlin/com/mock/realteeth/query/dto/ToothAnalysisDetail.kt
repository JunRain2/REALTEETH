package com.mock.realteeth.query.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus

data class ToothAnalysisDetail(
    val id: Long,
    val status: ToothAnalysisStatus,
    val jobId: String?,
    val imageUrl: String,
    val result: String?,
)
