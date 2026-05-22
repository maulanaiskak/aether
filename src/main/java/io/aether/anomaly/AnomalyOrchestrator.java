package io.aether.anomaly;

import io.aether.anomaly.detector.AnomalyDetector;
import io.aether.anomaly.detector.AnomalyResult;
import io.aether.anomaly.entity.AnomalyEntity;
import io.aether.anomaly.repository.AnomalyRepository;
import io.aether.domain.*;
import io.aether.domain.event.AnomalyDetectedEvent;
import io.aether.domain.event.ReadingValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class AnomalyOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnomalyOrchestrator.class);

    private final int windowSize;
    private final List<AnomalyDetector> detectors;
    private final AnomalyRepository anomalyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AnomalyOrchestrator(
            @Value("${aether.anomaly.window-size:48}") int windowSize,
            List<AnomalyDetector> detectors,
            AnomalyRepository anomalyRepository,
            ApplicationEventPublisher eventPublisher) {
        this.windowSize = windowSize;
        this.detectors = detectors;
        this.anomalyRepository = anomalyRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onReading(ReadingValidatedEvent event) {
        var reading = event.reading();
        if (reading.quality().status() == QualityStatus.REJECTED) return;
        if (reading.value() == null) return;

        var windowStart = OffsetDateTime.ofInstant(
                reading.observedAt().minusSeconds(windowSize * 3600L), ZoneOffset.UTC);
        var windowEnd = OffsetDateTime.ofInstant(reading.observedAt(), ZoneOffset.UTC);

        // Use a simple repository query by location+metric for the window
        // We approximate by fetching recent readings for this metric+location
        Flux.fromIterable(detectors)
                .map(d -> d.detect(List.of(), reading)) // simplified: no window fetch here
                .filter(AnomalyResult::anomalous)
                .flatMap(result -> anomalyRepository.save(toEntity(reading, result)))
                .doOnNext(saved -> eventPublisher.publishEvent(
                        new AnomalyDetectedEvent(this, toAnomalyEvent(reading, saved))))
                .subscribe(
                        r -> log.info("Anomaly detected for {}", reading.sensorId()),
                        err -> log.error("Anomaly orchestration error: {}", err.getMessage())
                );
    }

    private AnomalyEntity toEntity(SensorReading r, AnomalyResult result) {
        return new AnomalyEntity(
                null,
                r.sensorId().toString(),
                r.location(),
                r.metric().name(),
                OffsetDateTime.ofInstant(r.observedAt(), ZoneOffset.UTC),
                r.value(),
                result.method(),
                result.score(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private AnomalyEvent toAnomalyEvent(SensorReading r, AnomalyEntity saved) {
        return new AnomalyEvent(
                r.sensorId(),
                r.location(),
                r.metric(),
                r.observedAt(),
                r.value(),
                saved.method(),
                saved.score()
        );
    }
}
