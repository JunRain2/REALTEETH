package com.mock.realteeth.command.domain

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ToothAnalysisRepository : CoroutineCrudRepository<ToothAnalysis, Long> {
    fun findAllByStatusOrderByLastSentAtAsc(status: ToothAnalysisStatus): Flow<ToothAnalysis>
}
