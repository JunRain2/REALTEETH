package com.mock.realteeth.command.domain.exception

class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
