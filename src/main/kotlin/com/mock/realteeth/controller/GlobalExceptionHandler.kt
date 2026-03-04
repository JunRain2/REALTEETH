package com.mock.realteeth.controller

import com.mock.realteeth.controller.dto.ApiResponse
import com.mock.realteeth.domain.exception.BusinessException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ex.errorCode
        log.warn { "Business error: [${errorCode.name}] ${errorCode.message}" }
        return ResponseEntity.status(errorCode.status).body(
            ApiResponse.error(code = errorCode.name, message = errorCode.message),
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidation(ex: WebExchangeBindException): ResponseEntity<ApiResponse<Nothing>> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        log.warn { "Validation error: $details" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponse.error(message = "입력값이 올바르지 않습니다", details = details),
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        val details = ex.constraintViolations.map { "${it.propertyPath}: ${it.message}" }
        log.warn { "Constraint violation: $details" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponse.error(message = "입력값이 올바르지 않습니다", details = details),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        exchange: ServerWebExchange,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.error(ex) { "Unexpected error at ${exchange.request.path.value()}" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error(message = "서버에서 알 수 없는 오류가 발생했습니다."),
        )
    }
}
