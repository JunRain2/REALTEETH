package com.mock.realteeth.command.domain

// Mock Worker랑 통신할 도메인 서비스
interface ToothAnalysisClient {
    // AI에게 요청을 전송
    suspend fun requestAnalysis(toothImage: ToothImage): RequestAnalysisResult

    // AI에게 분석 상태를 요청
    suspend fun fetchStatus(toothAnalysis: ToothAnalysis): FetchStatusResult
}

data class RequestAnalysisResult(
    val analysisStatus: ToothAnalysisStatus,
    val jobId: String? = null,
    val result: String? = null,
)

data class FetchStatusResult(
    val analysisStatus: ToothAnalysisStatus,
    val result: String? = null,
)
