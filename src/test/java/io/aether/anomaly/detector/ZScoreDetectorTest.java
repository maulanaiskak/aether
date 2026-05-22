package io.aether.anomaly.detector;

import io.aether.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ZScoreDetectorTest {

    private final ZScoreDetector detector = new ZScoreDetector(3.0);

    private SensorReading reading(double value) {
        return new SensorReading(SensorId.parse("open-meteo:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³", value, Instant.now(), Instant.now(), "open-meteo", Quality.ok());
    }

    @Test
    void emptyWindowNotAnomalous() {
        assertThat(detector.detect(List.of(), reading(999)).anomalous()).isFalse();
    }

    @Test
    void constantSeriesNotAnomalous() {
        var window = IntStream.range(0, 20).mapToObj(i -> reading(25.0)).toList();
        assertThat(detector.detect(window, reading(25.0)).anomalous()).isFalse();
    }

    @Test
    void spikeIsAnomalous() {
        var window = IntStream.range(0, 20).mapToObj(i -> reading(25.0)).toList();
        assertThat(detector.detect(window, reading(200.0)).anomalous()).isTrue();
    }

    @Test
    void smallWindowReturnsFalse() {
        var window = List.of(reading(25.0), reading(26.0));
        assertThat(detector.detect(window, reading(999.0)).anomalous()).isFalse();
    }
}
