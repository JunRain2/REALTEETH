package com.mock.realteeth.command.domain.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),

    // 치아 분석
    TOOTH_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "치아 분석 정보를 찾을 수 없습니다"),
    TOOTH_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "치아 이미지를 찾을 수 없습니다"),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "락 획득에 실패했습니다"),
    TOOTH_ANALYSIS_NOT_REQUESTABLE(HttpStatus.CONFLICT, "분석 요청이 불가능한 상태입니다"),
    DATE_RANGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "조회 날짜 범위는 최대 6개월입니다"),
}
