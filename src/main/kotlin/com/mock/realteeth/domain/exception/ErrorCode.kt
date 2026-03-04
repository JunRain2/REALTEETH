package com.mock.realteeth.domain.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),

    // 예시 도메인 (추후 추가)
    // USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    // USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다"),
}
