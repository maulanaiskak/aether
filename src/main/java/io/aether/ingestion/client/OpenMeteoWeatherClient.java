package io.aether.ingestion.client;

import io.aether.domain.Location;
import io.aether.ingestion.dto.OpenMeteoWeatherResponseDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenMeteoWeatherClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoWeatherClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public OpenMeteoWeatherClient(
            @Qualifier("weatherWebClient") WebClient webClient,
            CircuitBreaker openMeteoCircuitBreaker,
            Retry openMeteoRetry) {
        this.webClient = webClient;
        this.circuitBreaker = openMeteoCircuitBreaker;
        this.retry = openMeteoRetry;
    }

    public Mono<OpenMeteoWeatherResponseDto> fetch(Location location) {
        Mono<OpenMeteoWeatherResponseDto> call = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", location.lat())
                        .queryParam("longitude", location.lon())
                        .queryParam("hourly", "temperature_2m,relative_humidity_2m,wind_speed_10m,surface_pressure")
                        .queryParam("timezone", "UTC")
                        .queryParam("forecast_days", "1")
                        .build())
                .retrieve()
                .bodyToMono(OpenMeteoWeatherResponseDto.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));

        return call.transformDeferred(RetryOperator.of(retry))
                .onErrorResume(CallNotPermittedException.class, e -> Mono.empty())
                .onErrorResume(e -> {
                    log.warn("Open-Meteo weather call failed for {}: {}", location.name(), e.getMessage());
                    return Mono.empty();
                });
    }
}
