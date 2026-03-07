package com.mock.realteeth.query.dto

import com.mock.realteeth.command.domain.ToothAnalysisStatus
import java.time.LocalDate

data class ListToothAnalysisQuery(
    val from: LocalDate,
    val to: LocalDate,
    val status: ToothAnalysisStatus?,
    val page: Int,
    val size: Int,
)
