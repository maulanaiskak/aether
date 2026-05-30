package io.aether.anomaly.repository;

import io.aether.anomaly.entity.AnomalyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

public interface AnomalyRepository extends ReactiveCrudRepository<AnomalyEntity, Long> {

    Flux<AnomalyEntity> findByLocationAndMetricAndObservedAtBetween(
            String location, String metric, OffsetDateTime from, OffsetDateTime to);

    @org.springframework.data.r2dbc.repository.Query("SELECT * FROM anomaly WHERE location = :location AND observed_at BETWEEN :from AND :to ORDER BY observed_at DESC LIMIT :limit")
    Flux<AnomalyEntity> findByLocationAndObservedAtBetween(
            String location, OffsetDateTime from, OffsetDateTime to, int limit);
}
