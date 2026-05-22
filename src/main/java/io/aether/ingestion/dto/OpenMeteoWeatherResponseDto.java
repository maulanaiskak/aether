package io.aether.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoWeatherResponseDto(
        double latitude,
        double longitude,
        String timezone,
        HourlyData hourly
) {}
