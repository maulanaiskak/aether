package io.aether.anomaly.repository;

import io.aether.anomaly.entity.AnomalyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

public interface AnomalyRepository extends ReactiveCrudRepository<AnomalyEntity, Long> {

    Flux<AnomalyEntity> findByLocationAndMetricAndObservedAtBetween(
            String location, String metric, OffsetDateTime from, OffsetDateTime to);
}
