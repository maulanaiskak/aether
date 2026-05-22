package io.aether.ingestion.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
class WebClientConfig {

    @Bean
    @Qualifier("weatherWebClient")
    WebClient weatherWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://api.open-meteo.com").build();
    }

    @Bean
    @Qualifier("airQualityWebClient")
    WebClient airQualityWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://air-quality-api.open-meteo.com").build();
    }
}
