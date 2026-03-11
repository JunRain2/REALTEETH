package com.mock.realteeth.command.domain

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ToothAnalysisRepository : CoroutineCrudRepository<ToothAnalysis, Long> {
    fun findAllByStatusOrderByLastSentAtAsc(status: ToothAnalysisStatus): Flow<ToothAnalysis>

    @Query("SELECT * FROM tooth_analyses WHERE image_id = :imageId AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 1")
    suspend fun findFirstActiveByImageId(imageId: Long): ToothAnalysis?
}
