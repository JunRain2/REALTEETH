package com.mock.realteeth.infra.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    @Bean
    fun toothAnalysisWebClient(
        @Value("\${client.tooth-analysis.base-url}") baseUrl: String,
        @Value("\${client.tooth-analysis.api-key}") apiKey: String,
    ): WebClient =
        WebClient
            .builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", apiKey)
            .build()
}
