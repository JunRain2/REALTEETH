package com.mock.realteeth.ui.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class AnalyzeToothRequest(
    @field:NotBlank
    @field:Schema(description = "분석할 치아 이미지 URL", example = "https://example.com/tooth.jpg")
    val imageUrl: String,
    @field:NotBlank
    @field:Schema(description = "이미지 MD5 해시값 (32자리 hex)", example = "d41d8cd98f00b204e9800998ecf8427e")
    val imageHash: String,
)
