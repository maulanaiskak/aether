package io.aether.api.handler;

import io.aether.anomaly.repository.AnomalyRepository;
import io.aether.api.dto.AnomalyEventDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class AnomalyHandler {

    private final AnomalyRepository anomalyRepo;

    public AnomalyHandler(AnomalyRepository anomalyRepo) {
        this.anomalyRepo = anomalyRepo;
    }

    public Mono<ServerResponse> queryAnomalies(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        var metric = request.queryParam("metric")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "metric required"));
        var from = parseInstant(request.queryParam("from").orElse(null), Instant.now().minusSeconds(3600 * 24));
        var to = parseInstant(request.queryParam("to").orElse(null), Instant.now());

        return ServerResponse.ok().body(
                anomalyRepo.findByLocationAndMetricAndObservedAtBetween(location, metric,
                        OffsetDateTime.ofInstant(from, ZoneOffset.UTC),
                        OffsetDateTime.ofInstant(to, ZoneOffset.UTC))
                        .map(e -> new AnomalyEventDto(e.sensorId(), e.location(), e.metric(),
                                e.observedAt().toInstant(), e.value(), e.method(), e.score())),
                AnomalyEventDto.class);
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (value == null) return fallback;
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timestamp: " + value);
        }
    }
}
