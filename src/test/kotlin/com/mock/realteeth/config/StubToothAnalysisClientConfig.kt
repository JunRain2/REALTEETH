package com.mock.realteeth.config

import com.mock.realteeth.command.domain.FetchStatusResult
import com.mock.realteeth.command.domain.RequestAnalysisResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisClient
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class StubToothAnalysisClientConfig {
    companion object {
        var requestAnalysisResult: RequestAnalysisResult =
            RequestAnalysisResult(ToothAnalysisStatus.PROCESSING, jobId = "stub-job-id")
        var fetchStatusResult: FetchStatusResult =
            FetchStatusResult(ToothAnalysisStatus.PROCESSING)
    }

    @Bean
    @Primary
    fun toothAnalysisClient(): ToothAnalysisClient =
        object : ToothAnalysisClient {
            override suspend fun requestAnalysis(toothImage: ToothImage): RequestAnalysisResult = requestAnalysisResult

            override suspend fun fetchStatus(toothAnalysis: ToothAnalysis): FetchStatusResult = fetchStatusResult
        }
}
