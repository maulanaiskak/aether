package io.aether.processing.validation;

import io.aether.domain.Metric;

import java.util.Map;

/** Physical plausibility bounds per metric. */
public final class PhysicalRange {

    public record Range(double min, double max) {}

    private static final Map<Metric, Range> RANGES = Map.ofEntries(
            Map.entry(Metric.TEMPERATURE,   new Range(-90, 60)),
            Map.entry(Metric.HUMIDITY,      new Range(0, 100)),
            Map.entry(Metric.WIND_SPEED,    new Range(0, 400)),
            Map.entry(Metric.PRESSURE,      new Range(870, 1085)),
            Map.entry(Metric.PM2_5,         new Range(0, 1000)),
            Map.entry(Metric.PM10,          new Range(0, 2000)),
            Map.entry(Metric.O3,            new Range(0, 1000)),
            Map.entry(Metric.NO2,           new Range(0, 2000)),
            Map.entry(Metric.SO2,           new Range(0, 2000)),
            Map.entry(Metric.CO,            new Range(0, 50000)),
            Map.entry(Metric.US_AQI,        new Range(0, 500)),
            Map.entry(Metric.EU_AQI,        new Range(0, 100))
    );

    private PhysicalRange() {}

    public static Range of(Metric metric) {
        return RANGES.getOrDefault(metric, new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
    }
}
