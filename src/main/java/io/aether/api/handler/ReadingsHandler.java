package io.aether.api.handler;

import io.aether.api.dto.DtoMapper;
import io.aether.api.dto.SensorReadingDto;
import io.aether.processing.repository.SensorReadingRepository;
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
public class ReadingsHandler {

    private final SensorReadingRepository repository;
    private final DtoMapper mapper;

    public ReadingsHandler(SensorReadingRepository repository, DtoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Mono<ServerResponse> queryReadings(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        var metric = request.queryParam("metric")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "metric required"));
        var from = parseInstant(request.queryParam("from").orElse(null), Instant.now().minusSeconds(3600));
        var to = parseInstant(request.queryParam("to").orElse(null), Instant.now());

        return ServerResponse.ok().body(
                repository.findByLocationAndMetricAndObservedAtBetween(location, metric,
                        OffsetDateTime.ofInstant(from, ZoneOffset.UTC),
                        OffsetDateTime.ofInstant(to, ZoneOffset.UTC))
                        .map(e -> new SensorReadingDto(
                                e.sensorId(), e.location(), e.metric(), e.unit(),
                                e.value(), e.smoothedValue(),
                                e.observedAt().toInstant(), e.qualityStatus())),
                SensorReadingDto.class);
    }

    public Mono<ServerResponse> latestReadings(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        return repository.findLatestByLocation(location)
                .map(e -> new SensorReadingDto(e.sensorId(), e.location(), e.metric(), e.unit(),
                        e.value(), e.smoothedValue(), e.observedAt().toInstant(), e.qualityStatus()))
                .flatMap(dto -> ServerResponse.ok().bodyValue(dto))
                .switchIfEmpty(ServerResponse.notFound().build());
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
