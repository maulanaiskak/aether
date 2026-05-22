package io.aether.forecast.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("forecast")
public record ForecastEntity(
        @Id Long id,
        String location,
        String metric,
        OffsetDateTime horizonAt,
        double predicted,
        double lowerBound,
        double upperBound,
        String model,
        OffsetDateTime createdAt
) {}
