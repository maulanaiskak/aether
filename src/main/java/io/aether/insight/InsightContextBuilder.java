package io.aether.insight;

import io.aether.anomaly.entity.AnomalyEntity;
import io.aether.anomaly.repository.AnomalyRepository;
import io.aether.domain.*;
import io.aether.forecast.entity.ForecastEntity;
import io.aether.forecast.repository.ForecastRepository;
import io.aether.processing.entity.SensorReadingEntity;
import io.aether.processing.repository.SensorReadingRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class InsightContextBuilder {

    private final SensorReadingRepository readingRepo;
    private final ForecastRepository forecastRepo;
    private final AnomalyRepository anomalyRepo;

    public InsightContextBuilder(
            SensorReadingRepository readingRepo,
            ForecastRepository forecastRepo,
            AnomalyRepository anomalyRepo) {
        this.readingRepo = readingRepo;
        this.forecastRepo = forecastRepo;
        this.anomalyRepo = anomalyRepo;
    }

    public Mono<InsightContext> build(Location location) {
        var now = Instant.now();
        var windowStart = OffsetDateTime.ofInstant(now.minusSeconds(12 * 3600), ZoneOffset.UTC);
        var windowEnd = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        var anomalyStart = OffsetDateTime.ofInstant(now.minusSeconds(2 * 3600), ZoneOffset.UTC);
        var forecastStart = windowEnd;

        Mono<List<SensorReading>> readings = readingRepo
                .findByLocationAndMetricAndObservedAtBetween(location.name(), Metric.PM2_5.name(), windowStart, windowEnd)
                .map(this::toReading)
                .collectList();

        Mono<List<ForecastPoint>> forecast = forecastRepo
                .findByLocationAndMetricAndHorizonAtAfter(location.name(), Metric.PM2_5.name(), forecastStart)
                .map(this::toForecast)
                .collectList();

        Mono<List<AnomalyEvent>> anomalies = anomalyRepo
                .findByLocationAndMetricAndObservedAtBetween(location.name(), Metric.PM2_5.name(), anomalyStart, windowEnd)
                .map(this::toAnomaly)
                .collectList();

        return Mono.zip(readings, forecast, anomalies)
                .map(t -> new InsightContext(t.getT1(), t.getT2(), t.getT3(), location, now));
    }

    private SensorReading toReading(SensorReadingEntity e) {
        return new SensorReading(
                SensorId.parse(e.sensorId()),
                e.schemaVersion(),
                e.location(),
                Metric.valueOf(e.metric()),
                e.unit(),
                e.value(),
                e.observedAt().toInstant(),
                e.ingestedAt().toInstant(),
                e.source(),
                new Quality(QualityStatus.valueOf(e.qualityStatus()), java.util.Set.of())
        );
    }

    private ForecastPoint toForecast(ForecastEntity e) {
        return new ForecastPoint(e.horizonAt().toInstant(), e.predicted(), e.lowerBound(), e.upperBound());
    }

    private AnomalyEvent toAnomaly(AnomalyEntity e) {
        return new AnomalyEvent(
                SensorId.parse(e.sensorId()),
                e.location(),
                Metric.valueOf(e.metric()),
                e.observedAt().toInstant(),
                e.value(),
                e.method(),
                e.score()
        );
    }
}
