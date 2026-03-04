package com.mock.realteeth.controller.dto

data class ApiError(
    val code: String? = null,
    val message: String,
    val details: List<String>? = null,
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
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
