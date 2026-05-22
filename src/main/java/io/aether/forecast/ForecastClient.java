package io.aether.forecast;

import io.aether.domain.Location;
import io.aether.forecast.dto.ForecastRequestDto;
import io.aether.forecast.dto.ForecastResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class ForecastClient {

    private static final Logger log = LoggerFactory.getLogger(ForecastClient.class);

    private final WebClient webClient;

    public ForecastClient(
            WebClient.Builder builder,
            @Value("${aether.ml-service.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<ForecastResponseDto> forecast(Location location, List<ForecastRequestDto.DataPoint> history) {
        var request = new ForecastRequestDto(location.name(), history);
        return webClient.post()
                .uri("/forecast")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ForecastResponseDto.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.warn("Forecast sidecar unavailable for {}: {}", location.name(), e.getMessage());
                    return Mono.empty();
                });
    }
}
