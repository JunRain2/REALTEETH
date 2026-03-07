package com.mock.realteeth.command.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("tooth_images")
class ToothImage(
    @Column("url") val url: String,
) : BaseEntity()
