package io.aether.domain;

public record SensorId(String source, String location, String metric) {

    public static SensorId parse(String raw) {
        var parts = raw.split(":", 3);
        if (parts.length != 3) throw new IllegalArgumentException("Invalid SensorId: " + raw);
        return new SensorId(parts[0], parts[1], parts[2]);
    }

    @Override
    public String toString() {
        return source + ":" + location + ":" + metric;
    }
}
