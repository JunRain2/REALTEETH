package com.mock.realteeth.ui.dto

import jakarta.validation.constraints.NotBlank

data class AnalyzeToothRequest(
    @field:NotBlank val imageUrl: String,
)
