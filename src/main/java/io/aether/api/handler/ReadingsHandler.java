package io.aether.api.handler;

import io.aether.api.dto.PageDto;
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

    public ReadingsHandler(SensorReadingRepository repository) {
        this.repository = repository;
    }

    public Mono<ServerResponse> queryReadings(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        var metric = request.queryParam("metric").orElse(null);
        var limit = Integer.parseInt(request.queryParam("limit").orElse("48"));
        var from = OffsetDateTime.ofInstant(Instant.now().minusSeconds(limit * 3600L), ZoneOffset.UTC);
        var to = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        var rows = metric != null
                ? repository.findByLocationAndMetricAndObservedAtBetween(location, metric, from, to)
                : repository.findByLocationAndObservedAtBetween(location, from, to, limit);

        return rows.map(this::toDto)
                .collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(new PageDto<>(list, list.size())));
    }

    public Mono<ServerResponse> latestReadings(ServerRequest request) {
        var location = request.queryParam("location")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
        return repository.findLatestByLocation(location)
                .map(e -> java.util.List.of(toDto(e)))
                .flatMap(list -> ServerResponse.ok().bodyValue(list))
                .switchIfEmpty(ServerResponse.ok().bodyValue(java.util.List.of()));
    }

    private SensorReadingDto toDto(io.aether.processing.entity.SensorReadingEntity e) {
        return new SensorReadingDto(
                e.sensorId(), e.location(), e.metric(), e.unit(),
                e.value(), e.smoothedValue(),
                e.observedAt().toInstant().toString(),
                e.qualityStatus());
    }
}
