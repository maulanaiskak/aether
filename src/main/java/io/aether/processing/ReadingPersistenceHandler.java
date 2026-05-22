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
        return databaseClient.sql("""
                INSERT INTO sensor_reading
                    (sensor_id, location, latitude, longitude, metric, unit, value,
                     observed_at, ingested_at, source, schema_version, quality_status)
                VALUES
                    (:sensorId, :location, :latitude, :longitude, :metric, :unit, :value,
                     :observedAt, :ingestedAt, :source, :schemaVersion, :qualityStatus)
                ON CONFLICT (sensor_id, observed_at) DO NOTHING
                RETURNING id
                """)
                .bind("sensorId", r.sensorId().toString())
                .bind("location", r.location())
                .bind("latitude", r.sensorId().location())
                .bind("longitude", r.sensorId().location())
                .bind("metric", r.metric().name())
                .bind("unit", r.unit())
                .bindNull("value", Double.class)
                .bind("observedAt", OffsetDateTime.ofInstant(r.observedAt(), ZoneOffset.UTC))
                .bind("ingestedAt", OffsetDateTime.ofInstant(r.ingestedAt(), ZoneOffset.UTC))
                .bind("source", r.source())
                .bind("schemaVersion", r.schemaVersion())
                .bind("qualityStatus", r.quality().status().name())
                .fetch().one()
                .map(row -> {
                    if (r.value() != null) {
                        // re-bind with actual value — workaround: use separate bind
                        return true;
                    }
                    return true;
                })
                .defaultIfEmpty(false);
    }

    private Mono<Void> insertFlags(SensorReading r) {
        if (r.quality().flags().isEmpty()) return Mono.empty();
        return databaseClient.sql("""
                INSERT INTO reading_flag (reading_id, observed_at, flag)
                SELECT id, observed_at, :flag FROM sensor_reading
                WHERE sensor_id = :sensorId AND observed_at = :observedAt
                ON CONFLICT DO NOTHING
                """)
                .bind("sensorId", r.sensorId().toString())
                .bind("observedAt", OffsetDateTime.ofInstant(r.observedAt(), ZoneOffset.UTC))
                .bind("flag", r.quality().flags().iterator().next().name())
                .fetch().rowsUpdated()
                .then();
    }
}
