package com.mock.realteeth.infra.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClientResponseException

@Configuration
class ResilienceConfig(
    @Value("\${app.resilience.circuit-breaker.sliding-window-size:5}") private val slidingWindowSize: Int,
    @Value("\${app.resilience.circuit-breaker.failure-rate-threshold:60}") private val failureRateThreshold: Float,
    @Value("\${app.resilience.circuit-breaker.wait-duration-in-open-state-seconds:30}") private val waitDurationSeconds: Long,
    @Value("\${app.resilience.circuit-breaker.permitted-number-of-calls-in-half-open-state:1}") private val permittedCallsInHalfOpen: Int,
) {
    @Bean
    fun toothAnalysisCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker {
        val config =
            CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationSeconds))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpen)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException { e ->
                    when (e) {
                        is WebClientResponseException -> e.statusCode.value() !in listOf(400, 422)
                        else -> true
                    }
                }
                .build()
        return registry.circuitBreaker("toothAnalysis", config)
    }
}
