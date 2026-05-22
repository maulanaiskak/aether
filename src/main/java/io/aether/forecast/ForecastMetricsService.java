package io.aether.forecast;

import io.aether.forecast.entity.ForecastMetricsEntity;
import io.aether.forecast.repository.ForecastMetricsRepository;
import io.aether.forecast.repository.ForecastRepository;
import io.aether.processing.repository.SensorReadingRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ForecastMetricsService {

    private final ForecastRepository forecastRepo;
    private final SensorReadingRepository readingRepo;
    private final ForecastMetricsRepository metricsRepo;

    public ForecastMetricsService(
            ForecastRepository forecastRepo,
            SensorReadingRepository readingRepo,
            ForecastMetricsRepository metricsRepo) {
        this.forecastRepo = forecastRepo;
        this.readingRepo = readingRepo;
        this.metricsRepo = metricsRepo;
    }

    public Mono<Void> evaluateAndStore(String location, String model) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var since = now.minusHours(25);

        return forecastRepo.findByLocationAndMetricAndHorizonAtAfter(location, "PM2_5", since)
                .collectList()
                .flatMap(forecasts -> {
                    if (forecasts.isEmpty()) return Mono.empty();
                    double[] diffs = forecasts.stream()
                            .mapToDouble(f -> Math.abs(f.predicted() - f.predicted()))
                            .toArray();
                    double mae = java.util.Arrays.stream(diffs).average().orElse(0);
                    double rmse = Math.sqrt(java.util.Arrays.stream(diffs)
                            .map(d -> d * d).average().orElse(0));

                    var entity = new ForecastMetricsEntity(null, location, "PM2_5", model, mae, rmse, now);
                    return metricsRepo.save(entity).then();
                });
    }
}
