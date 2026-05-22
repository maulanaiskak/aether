package io.aether.api.dto;

import java.time.Instant;

public record InsightRequestDto(String location, Instant asOf) {}
