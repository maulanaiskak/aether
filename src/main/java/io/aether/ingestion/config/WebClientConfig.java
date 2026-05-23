package io.aether.ingestion.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
class WebClientConfig {

    @Bean
    @Qualifier("weatherWebClient")
    WebClient weatherWebClient(WebClient.Builder builder,
            @Value("${aether.ingestion.weather-base-url:https://api.open-meteo.com}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("airQualityWebClient")
    WebClient airQualityWebClient(WebClient.Builder builder,
            @Value("${aether.ingestion.aq-base-url:https://air-quality-api.open-meteo.com}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
