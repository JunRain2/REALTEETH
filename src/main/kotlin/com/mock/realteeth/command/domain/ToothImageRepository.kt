package com.mock.realteeth.command.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ToothImageRepository : CoroutineCrudRepository<ToothImage, Long> {
    suspend fun findByImageHash(imageHash: String): ToothImage?
}
