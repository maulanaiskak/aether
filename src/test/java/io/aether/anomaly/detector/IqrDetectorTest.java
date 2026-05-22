package io.aether.anomaly.detector;

import io.aether.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class IqrDetectorTest {

    private final IqrDetector detector = new IqrDetector(1.5);

    private SensorReading reading(double value) {
        return new SensorReading(SensorId.parse("open-meteo:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³", value, Instant.now(), Instant.now(), "open-meteo", Quality.ok());
    }

    @Test
    void normalValueNotAnomalous() {
        var window = IntStream.rangeClosed(20, 30).mapToObj(i -> reading(i)).toList();
        assertThat(detector.detect(window, reading(25.0)).anomalous()).isFalse();
    }

    @Test
    void outlierIsAnomalous() {
        var window = IntStream.rangeClosed(20, 30).mapToObj(i -> reading(i)).toList();
        assertThat(detector.detect(window, reading(5000.0)).anomalous()).isTrue();
    }
}
