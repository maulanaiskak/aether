package io.aether.api.dto;

import java.time.Instant;

public record InsightResponseDto(String location, Instant asOf, String summary, String provider) {}
