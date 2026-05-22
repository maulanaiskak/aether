package io.aether.forecast.repository;

import io.aether.forecast.entity.ForecastEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

public interface ForecastRepository extends ReactiveCrudRepository<ForecastEntity, Long> {

    Flux<ForecastEntity> findByLocationAndMetricAndHorizonAtAfter(
            String location, String metric, OffsetDateTime after);
}
