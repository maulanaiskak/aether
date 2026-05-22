package io.aether.anomaly.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("anomaly")
public record AnomalyEntity(
        @Id Long id,
        String sensorId,
        String location,
        String metric,
        OffsetDateTime observedAt,
        Double value,
        String method,
        double score,
        OffsetDateTime createdAt
) {}
