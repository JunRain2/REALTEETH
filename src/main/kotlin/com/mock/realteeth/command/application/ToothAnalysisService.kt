package com.mock.realteeth.command.application

import com.mock.realteeth.command.application.dto.AnalyzeToothCommand
import com.mock.realteeth.command.application.dto.AnalyzeToothResult
import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisClient
import com.mock.realteeth.command.domain.ToothAnalysisRepository
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.ToothImage
import com.mock.realteeth.command.domain.ToothImageRepository
import com.mock.realteeth.command.domain.exception.BusinessException
import com.mock.realteeth.command.domain.exception.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class ToothAnalysisService(
    private val toothImageRepository: ToothImageRepository,
    private val toothAnalysisRepository: ToothAnalysisRepository,
    private val toothAnalysisClient: ToothAnalysisClient,
    @param:Value("\${app.tooth-analysis.max-send-count}") private val maxSendCount: Int,
) {
    @Transactional
    suspend fun analyzeTooth(command: AnalyzeToothCommand): AnalyzeToothResult {
        val toothImage = toothImageRepository.save(ToothImage(command.imageUrl))
        val toothAnalysis = toothAnalysisRepository.save(ToothAnalysis.prepare(toothImage))

        return AnalyzeToothResult(
            toothAnalysisId = toothAnalysis.id,
            toothAnalysisStatus = toothAnalysis.status,
        )
    }

    suspend fun processPendingAnalyses() {
        toothAnalysisRepository.findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PENDING).collect { toothAnalysis ->
            runCatching {
                if (toothAnalysis.isSendLimitExceeded(maxSendCount)) {
                    toothAnalysis.markAsFailed("최대 전송 횟수(${maxSendCount}회) 초과")
                } else {
                    val toothImage =
                        toothImageRepository.findById(toothAnalysis.imageId) ?: throw BusinessException(ErrorCode.TOOTH_IMAGE_NOT_FOUND)
                    toothAnalysis.applyAnalysisResult(toothAnalysisClient.requestAnalysis(toothImage))
                }
                toothAnalysisRepository.save(toothAnalysis)
            }.onFailure { log.warn(it) { "processPendingAnalyses failed for id=${toothAnalysis.id}" } }
        }
    }

    suspend fun processProcessingAnalyses() {
        toothAnalysisRepository.findAllByStatusOrderByLastSentAtAsc(ToothAnalysisStatus.PROCESSING).collect { toothAnalysis ->
            runCatching {
                if (toothAnalysis.isSendLimitExceeded(maxSendCount)) {
                    toothAnalysis.markAsFailed("최대 전송 횟수(${maxSendCount}회) 초과")
                } else {
                    toothAnalysis.applyFetchStatusResult(toothAnalysisClient.fetchStatus(toothAnalysis))
                }
                toothAnalysisRepository.save(toothAnalysis)
            }.onFailure { log.warn(it) { "processProcessingAnalyses failed for id=${toothAnalysis.id}" } }
        }
    }
}
