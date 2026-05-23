package io.aether.processing.repository;

import io.aether.processing.entity.SensorReadingEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface SensorReadingRepository extends ReactiveCrudRepository<SensorReadingEntity, Long> {

    Flux<SensorReadingEntity> findByLocationAndMetricAndObservedAtBetween(
            String location, String metric, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT * FROM sensor_reading WHERE location = :location ORDER BY observed_at DESC LIMIT 1")
    Mono<SensorReadingEntity> findLatestByLocation(String location);

    Flux<SensorReadingEntity> findBySensorIdAndObservedAtGreaterThanEqualOrderByObservedAtAsc(
            String sensorId, OffsetDateTime since);
}
