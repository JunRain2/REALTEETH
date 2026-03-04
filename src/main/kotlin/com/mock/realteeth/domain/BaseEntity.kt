package com.mock.realteeth.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime

abstract class BaseEntity {
    @Id
    var id: Long = 0L
        private set

    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
        private set

    @LastModifiedDate
    @Column("updated_at")
    var updatedAt: LocalDateTime? = null
        private set

    @Column("deleted_at")
    var deletedAt: LocalDateTime? = null
        private set

    fun delete() {
        deletedAt = LocalDateTime.now()
    }
}
