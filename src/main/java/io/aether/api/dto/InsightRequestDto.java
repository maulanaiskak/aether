package io.aether.api.dto;

public record InsightRequestDto(String location, String metric, int windowHours) {}
