package io.aether.processing;

import io.aether.domain.*;
import io.aether.domain.event.ReadingValidatedEvent;
import io.aether.processing.validation.ValidationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ReadingPersistenceHandler {

    private static final Logger log = LoggerFactory.getLogger(ReadingPersistenceHandler.class);

    private final DatabaseClient databaseClient;
    private final ValidationPipeline validationPipeline;
    private final ApplicationEventPublisher eventPublisher;

    public ReadingPersistenceHandler(
            DatabaseClient databaseClient,
            ValidationPipeline validationPipeline,
            ApplicationEventPublisher eventPublisher) {
        this.databaseClient = databaseClient;
        this.validationPipeline = validationPipeline;
        this.eventPublisher = eventPublisher;
    }

    public Mono<SensorReading> processReading(SensorReading raw) {
        var validated = validationPipeline.validate(raw);
        return upsert(validated)
                .flatMap(inserted -> {
                    if (inserted) return insertFlags(validated);
                    return Mono.empty();
                })
                .then(Mono.fromRunnable(() ->
                        eventPublisher.publishEvent(new ReadingValidatedEvent(this, validated))))
                .thenReturn(validated);
    }

    private Mono<Boolean> upsert(SensorReading r) {
        var observedAt = OffsetDateTime.ofInstant(r.observedAt(), ZoneOffset.UTC);
        var ingestedAt = OffsetDateTime.ofInstant(r.ingestedAt(), ZoneOffset.UTC);
        var spec = databaseClient.sql(
                "INSERT INTO sensor_reading " +
                "(sensor_id, location, latitude, longitude, metric, unit, value, " +
                " observed_at, ingested_at, source, schema_version, quality_status) " +
                "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12) " +
                "ON CONFLICT (sensor_id, observed_at) DO NOTHING " +
                "RETURNING id")
                .bind(0, r.sensorId().toString())
                .bind(1, r.location())
                .bind(2, 0.0)
                .bind(3, 0.0)
                .bind(4, r.metric().name())
                .bind(5, r.unit())
                .bind(7, observedAt)
                .bind(8, ingestedAt)
                .bind(9, r.source())
                .bind(10, r.schemaVersion())
                .bind(11, r.quality().status().name());

        if (r.value() != null) {
            spec = spec.bind(6, r.value());
        } else {
            spec = spec.bindNull(6, Double.class);
        }

        return spec.fetch().one()
                .map(row -> true)
                .defaultIfEmpty(false);
    }

    private Mono<Void> insertFlags(SensorReading r) {
        if (r.quality().flags().isEmpty()) return Mono.empty();
        var observedAt = OffsetDateTime.ofInstant(r.observedAt(), ZoneOffset.UTC);
        return databaseClient.sql(
                "INSERT INTO reading_flag (reading_id, observed_at, flag) " +
                "SELECT id, observed_at, $1 FROM sensor_reading " +
                "WHERE sensor_id = $2 AND observed_at = $3 " +
                "ON CONFLICT DO NOTHING")
                .bind(0, r.quality().flags().iterator().next().name())
                .bind(1, r.sensorId().toString())
                .bind(2, observedAt)
                .fetch().rowsUpdated()
                .then();
    }
}
