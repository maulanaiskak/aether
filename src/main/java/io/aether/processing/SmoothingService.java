package io.aether.processing;

import io.aether.domain.event.ReadingValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SmoothingService {

    private static final Logger log = LoggerFactory.getLogger(SmoothingService.class);

    private final double alpha;
    private final DatabaseClient databaseClient;
    private final ConcurrentHashMap<String, Double> ewmaState = new ConcurrentHashMap<>();

    public SmoothingService(
            @Value("${aether.smoothing.alpha:0.2}") double alpha,
            DatabaseClient databaseClient) {
        this.alpha = alpha;
        this.databaseClient = databaseClient;
    }

    @EventListener
    public void onReadingValidated(ReadingValidatedEvent event) {
        var reading = event.reading();
        if (reading.value() == null) return;

        var sensorId = reading.sensorId().toString();
        var smoothed = ewmaState.merge(sensorId, reading.value(),
                (prev, curr) -> alpha * curr + (1 - alpha) * prev);

        databaseClient.sql("UPDATE sensor_reading SET smoothed_value = $1 WHERE sensor_id = $2 AND observed_at = $3")
                .bind(0, smoothed)
                .bind(1, sensorId)
                .bind(2, OffsetDateTime.ofInstant(reading.observedAt(), ZoneOffset.UTC))
                .fetch().rowsUpdated()
                .subscribe(rows -> {}, err -> log.error("Failed to update smoothed value: {}", err.getMessage()));
    }
}
