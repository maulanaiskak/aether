package io.aether.forecast.repository;

import io.aether.forecast.entity.ForecastMetricsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ForecastMetricsRepository extends ReactiveCrudRepository<ForecastMetricsEntity, Long> {

    Flux<ForecastMetricsEntity> findByLocationAndModel(String location, String model);
}
