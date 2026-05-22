package io.aether.forecast.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("forecast_metrics")
public record ForecastMetricsEntity(
        @Id Long id,
        String location,
        String metric,
        String model,
        double mae,
        double rmse,
        OffsetDateTime evaluatedAt
) {}
