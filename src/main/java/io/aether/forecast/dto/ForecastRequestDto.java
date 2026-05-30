package io.aether.forecast.dto;

import java.util.List;

public record ForecastRequestDto(
        String location,
        List<DataPoint> history
) {
    public record DataPoint(String observedAt, Double value) {}
}
