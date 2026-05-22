package io.aether.forecast;

import io.aether.domain.Metric;
import io.aether.domain.event.PollCycleCompletedEvent;
import io.aether.forecast.dto.ForecastRequestDto;
import io.aether.forecast.dto.ForecastResponseDto;
import io.aether.forecast.entity.ForecastEntity;
import io.aether.forecast.repository.ForecastRepository;
import io.aether.processing.repository.SensorReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class ForecastOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ForecastOrchestrator.class);

    private final ForecastClient forecastClient;
    private final SensorReadingRepository readingRepo;
    private final DatabaseClient databaseClient;
    private final ForecastMetricsService metricsService;

    public ForecastOrchestrator(
            ForecastClient forecastClient,
            SensorReadingRepository readingRepo,
            DatabaseClient databaseClient,
            ForecastMetricsService metricsService) {
        this.forecastClient = forecastClient;
        this.readingRepo = readingRepo;
        this.databaseClient = databaseClient;
        this.metricsService = metricsService;
    }

    @EventListener
    public void onPollCycleCompleted(PollCycleCompletedEvent event) {
        var location = event.location();
        var since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(72);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        readingRepo.findByLocationAndMetricAndObservedAtBetween(location.name(), Metric.PM2_5.name(), since, now)
                .map(e -> new ForecastRequestDto.DataPoint(e.observedAt().toString(), e.value()))
                .collectList()
                .flatMap(history -> forecastClient.forecast(location, history))
                .flatMap(response -> upsertForecasts(response, location.name()))
                .then(metricsService.evaluateAndStore(location.name(), "lightgbm"))
                .subscribe(
                        v -> {},
                        err -> log.error("Forecast orchestration failed for {}: {}", location.name(), err.getMessage())
                );
    }

    private Mono<Void> upsertForecasts(ForecastResponseDto response, String locationName) {
        return Mono.fromRunnable(() -> {
            for (var p : response.predictions()) {
                databaseClient.sql("""
                        INSERT INTO forecast (location, metric, horizon_at, predicted, lower_bound, upper_bound, model)
                        VALUES (:location, 'PM2_5', :horizonAt, :predicted, :lower, :upper, :model)
                        ON CONFLICT (location, metric, horizon_at)
                        DO UPDATE SET predicted = EXCLUDED.predicted,
                                     lower_bound = EXCLUDED.lower_bound,
                                     upper_bound = EXCLUDED.upper_bound,
                                     model = EXCLUDED.model,
                                     created_at = now()
                        """)
                        .bind("location", locationName)
                        .bind("horizonAt", OffsetDateTime.parse(p.horizonAt()))
                        .bind("predicted", p.predicted())
                        .bind("lower", p.lowerBound())
                        .bind("upper", p.upperBound())
                        .bind("model", response.model())
                        .fetch().rowsUpdated()
                        .subscribe();
            }
        });
    }
}
