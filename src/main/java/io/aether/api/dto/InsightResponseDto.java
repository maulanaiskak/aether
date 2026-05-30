package io.aether.api.dto;

import java.util.List;

public record InsightResponseDto(
        String location,
        String metric,
        String provider,
        List<InsightDto> insights,
        String generatedAt
) {
    public record InsightDto(String type, String severity, String title, String description) {}
}
