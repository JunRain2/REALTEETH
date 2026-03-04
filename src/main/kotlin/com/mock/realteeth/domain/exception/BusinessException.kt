package com.mock.realteeth.domain.exception

class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
