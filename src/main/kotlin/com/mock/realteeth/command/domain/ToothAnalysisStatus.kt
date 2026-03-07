package com.mock.realteeth.command.domain

enum class ToothAnalysisStatus {
    PENDING, // 대기 중
    PROCESSING, // 분석 중
    COMPLETED, // 완료
    FAILED, // 최종 실패
}
