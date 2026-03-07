package com.mock.realteeth.command.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("tooth_analyses")
class ToothAnalysis(
    @Column("image_id") val imageId: Long,
) : BaseEntity() {
    @Column("status")
    var status: ToothAnalysisStatus = ToothAnalysisStatus.PENDING
        private set

    @Column("job_id")
    var jobId: String? = null // MockWorker에서 리턴받은 JobId
        private set

    @Column("send_count")
    var sendCount: Int = 0
        private set

    @Column("result")
    var result: String? = null
        private set

    @Column("last_sent_at")
    var lastSentAt: LocalDateTime = LocalDateTime.now()
        private set

    companion object {
        fun prepare(toothImage: ToothImage) = ToothAnalysis(toothImage.id)
    }

    fun isSendLimitExceeded(maxCount: Int): Boolean = sendCount >= maxCount

    fun markAsFailed(cause: String?) {
        status = ToothAnalysisStatus.FAILED
        result = cause
    }

    fun isAnalysisEnded(): Boolean = status == ToothAnalysisStatus.COMPLETED || status == ToothAnalysisStatus.FAILED

    fun applyFetchStatusResult(fetchResult: FetchStatusResult) {
        status = fetchResult.analysisStatus
        result = fetchResult.result
        lastSentAt = LocalDateTime.now()
    }

    fun applyAnalysisResult(analysisResult: RequestAnalysisResult) {
        status = analysisResult.analysisStatus
        jobId = analysisResult.jobId ?: jobId
        result = analysisResult.result
        sendCount++
        lastSentAt = LocalDateTime.now()
    }
}
