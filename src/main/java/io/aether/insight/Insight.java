package io.aether.insight;

import java.time.Instant;

public record Insight(String location, Instant asOf, String summary, String provider) {}
