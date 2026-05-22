package io.aether.processing.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("sensor_reading")
public record SensorReadingEntity(
        @Id Long id,
        String sensorId,
        String location,
        double latitude,
        double longitude,
        String metric,
        String unit,
        Double value,
        Double smoothedValue,
        OffsetDateTime observedAt,
        OffsetDateTime ingestedAt,
        String source,
        int schemaVersion,
        String qualityStatus
) {}
