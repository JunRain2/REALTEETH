package com.mock.realteeth.query.application

import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.command.domain.exception.BusinessException
import com.mock.realteeth.command.domain.exception.ErrorCode
import com.mock.realteeth.query.dao.ToothAnalysisQueryRepository
import com.mock.realteeth.query.dto.GetToothAnalysisResult
import com.mock.realteeth.query.dto.ListToothAnalysisQuery
import com.mock.realteeth.query.dto.ListToothAnalysisResult
import com.mock.realteeth.query.dto.StreamAnalysisStatusResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds

@Service
class ToothAnalysisQueryService(
    private val toothAnalysisQueryRepository: ToothAnalysisQueryRepository,
    @param:Value("\${app.tooth-analysis.status-poll-interval-ms}") private val statusPollIntervalMs: Long,
) {
    fun streamAnalysisStatus(toothAnalysisId: Long): Flow<StreamAnalysisStatusResult> =
        flow {
            var detail =
                toothAnalysisQueryRepository.findDetailById(toothAnalysisId)
                    ?: throw BusinessException(ErrorCode.TOOTH_ANALYSIS_NOT_FOUND)

            emit(
                StreamAnalysisStatusResult(
                    detail.id,
                    detail.status,
                    detail.jobId,
                    detail.imageUrl,
                    detail.result,
                ),
            )
            var lastStatus = detail.status

            while (detail.status != ToothAnalysisStatus.COMPLETED && detail.status != ToothAnalysisStatus.FAILED) {
                delay(statusPollIntervalMs.milliseconds)
                detail =
                    toothAnalysisQueryRepository.findDetailById(toothAnalysisId)
                        ?: throw BusinessException(ErrorCode.TOOTH_ANALYSIS_NOT_FOUND)
                if (detail.status != lastStatus) {
                    lastStatus = detail.status
                    emit(
                        StreamAnalysisStatusResult(
                            detail.id,
                            detail.status,
                            detail.jobId,
                            detail.imageUrl,
                            detail.result,
                        ),
                    )
                }
            }
        }

    suspend fun getToothAnalysis(toothAnalysisId: Long): GetToothAnalysisResult {
        val detail =
            toothAnalysisQueryRepository.findDetailById(toothAnalysisId)
                ?: throw BusinessException(ErrorCode.TOOTH_ANALYSIS_NOT_FOUND)
        return GetToothAnalysisResult(
            toothAnalysisId = detail.id,
            toothAnalysisStatus = detail.status,
            jobId = detail.jobId,
            imageUrl = detail.imageUrl,
            result = detail.result,
        )
    }

    suspend fun listToothAnalyses(query: ListToothAnalysisQuery): ListToothAnalysisResult {
        if (query.from.isAfter(query.to)) throw BusinessException(ErrorCode.INVALID_INPUT)
        if (query.to.isAfter(query.from.plusMonths(6))) throw BusinessException(ErrorCode.DATE_RANGE_TOO_LARGE)

        val from = query.from.atStartOfDay()
        val to = query.to.plusDays(1).atStartOfDay()
        val offset = query.page.toLong() * query.size

        val rows =
            if (query.status != null) {
                toothAnalysisQueryRepository.findPageByStatus(from, to, query.status, query.size, offset)
            } else {
                toothAnalysisQueryRepository.findPage(from, to, query.size, offset)
            }.toList()

        val total =
            if (query.status != null) {
                toothAnalysisQueryRepository.countPageByStatus(from, to, query.status)
            } else {
                toothAnalysisQueryRepository.countPage(from, to)
            }

        return ListToothAnalysisResult(
            items = rows,
            totalCount = total,
            page = query.page,
            size = query.size,
        )
    }
}
