package io.aether.api.handler;

import io.aether.api.dto.ForecastMetricsDto;
import io.aether.api.dto.ForecastPointDto;
import io.aether.forecast.repository.ForecastMetricsRepository;
import io.aether.forecast.repository.ForecastRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ForecastHandler {

    private final ForecastRepository forecastRepo;
    private final ForecastMetricsRepository metricsRepo;

    public ForecastHandler(ForecastRepository forecastRepo, ForecastMetricsRepository metricsRepo) {
        this.forecastRepo = forecastRepo;
        this.metricsRepo = metricsRepo;
    }

    public Mono<ServerResponse> getForecast(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        var metric = request.queryParam("metric").orElse("PM2_5");
        var after = OffsetDateTime.now(ZoneOffset.UTC);

        return ServerResponse.ok().body(
                forecastRepo.findByLocationAndMetricAndHorizonAtAfter(location, metric, after)
                        .map(e -> new ForecastPointDto(e.horizonAt().toInstant(), e.predicted(), e.lowerBound(), e.upperBound())),
                ForecastPointDto.class);
    }

    public Mono<ServerResponse> getForecastMetrics(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        var model = request.queryParam("model").orElse("lightgbm");

        return ServerResponse.ok().body(
                metricsRepo.findByLocationAndModel(location, model)
                        .map(e -> new ForecastMetricsDto(e.location(), e.metric(), e.model(), e.mae(), e.rmse(), e.evaluatedAt().toInstant())),
                ForecastMetricsDto.class);
    }
}
