package io.aether.api.handler;

import io.aether.anomaly.repository.AnomalyRepository;
import io.aether.api.dto.AnomalyEventDto;
import io.aether.api.dto.PageDto;
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
        var metric = request.queryParam("metric").orElse(null);
        var limit = Integer.parseInt(request.queryParam("limit").orElse("20"));
        var from = OffsetDateTime.ofInstant(Instant.now().minusSeconds(3600L * 24), ZoneOffset.UTC);
        var to = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        var rows = metric != null
                ? anomalyRepo.findByLocationAndMetricAndObservedAtBetween(location, metric, from, to)
                : anomalyRepo.findByLocationAndObservedAtBetween(location, from, to, limit);

        return rows.map(e -> new AnomalyEventDto(e.sensorId(), e.location(), e.metric(),
                        e.observedAt().toInstant().toString(), e.value(), e.method(), e.score()))
                .collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(new PageDto<>(list, list.size())));
    }
}
