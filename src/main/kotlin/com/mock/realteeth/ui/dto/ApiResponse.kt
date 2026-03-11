package com.mock.realteeth.ui.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ApiError(
    @field:Schema(description = "에러 코드", example = "TOOTH_ANALYSIS_NOT_FOUND", nullable = true)
    val code: String? = null,
    @field:Schema(description = "에러 메시지", example = "치아 분석 결과를 찾을 수 없습니다.")
    val message: String,
    @field:Schema(description = "유효성 검증 오류 상세 목록", nullable = true)
    val details: List<String>? = null,
)

data class ApiResponse<T>(
    @field:Schema(description = "요청 성공 여부", example = "true")
    val success: Boolean,
    @field:Schema(description = "응답 데이터 (성공 시 존재)", nullable = true)
    val data: T? = null,
    @field:Schema(description = "에러 정보 (실패 시 존재)", nullable = true)
    val error: ApiError? = null,
) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data)

        fun ok() = ApiResponse<Nothing>(success = true)

        fun error(
            code: String? = null,
            message: String,
            details: List<String>? = null,
        ) = ApiResponse<Nothing>(success = false, error = ApiError(code, message, details))
    }
}
