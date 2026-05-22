package io.aether.ingestion.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
class ResilienceConfig {

    @Bean
    CircuitBreaker openMeteoCircuitBreaker() {
        return CircuitBreaker.of("open-meteo", CircuitBreakerConfig.custom()
                .slidingWindowSize(3)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(1)
                .build());
    }

    @Bean
    Retry openMeteoRetry() {
        return Retry.of("open-meteo", RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(2), 2.0))
                .build());
    }
}
