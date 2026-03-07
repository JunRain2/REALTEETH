package com.mock.realteeth.query.repository

import com.mock.realteeth.command.domain.ToothAnalysis
import com.mock.realteeth.command.domain.ToothAnalysisStatus
import com.mock.realteeth.query.dto.ListToothAnalysisItem
import com.mock.realteeth.query.dto.ToothAnalysisDetail
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface ToothAnalysisQueryRepository : CoroutineCrudRepository<ToothAnalysis, Long> {
    @Query(
        """
        SELECT ta.id, ta.status, ta.job_id, ta.result, ta.created_at, ti.url AS image_url
        FROM tooth_analyses ta JOIN tooth_images ti ON ta.image_id = ti.id
        WHERE ta.id = :id AND ta.deleted_at IS NULL
        """,
    )
    suspend fun findDetailById(id: Long): ToothAnalysisDetail?

    @Query(
        """
        SELECT ta.id, ta.status, ta.result, ta.created_at, ti.url AS image_url
        FROM tooth_analyses ta JOIN tooth_images ti ON ta.image_id = ti.id
        WHERE ta.deleted_at IS NULL AND ta.created_at >= :from AND ta.created_at < :to
        ORDER BY ta.created_at DESC LIMIT :limit OFFSET :offset
        """,
    )
    fun findPage(
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int,
        offset: Long,
    ): Flow<ListToothAnalysisItem>

    @Query(
        """
        SELECT ta.id, ta.status, ta.result, ta.created_at, ti.url AS image_url
        FROM tooth_analyses ta JOIN tooth_images ti ON ta.image_id = ti.id
        WHERE ta.deleted_at IS NULL AND ta.created_at >= :from AND ta.created_at < :to AND ta.status = :status
        ORDER BY ta.created_at DESC LIMIT :limit OFFSET :offset
        """,
    )
    fun findPageByStatus(
        from: LocalDateTime,
        to: LocalDateTime,
        status: ToothAnalysisStatus,
        limit: Int,
        offset: Long,
    ): Flow<ListToothAnalysisItem>

    @Query(
        "SELECT COUNT(*) FROM tooth_analyses WHERE deleted_at IS NULL AND created_at >= :from AND created_at < :to",
    )
    suspend fun countPage(
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long

    @Query(
        """
        SELECT COUNT(*) FROM tooth_analyses
        WHERE deleted_at IS NULL AND created_at >= :from AND created_at < :to AND status = :status
        """,
    )
    suspend fun countPageByStatus(
        from: LocalDateTime,
        to: LocalDateTime,
        status: ToothAnalysisStatus,
    ): Long
}
