package io.aether.insight;

import io.aether.domain.*;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedInsightProviderTest {

    private final RuleBasedInsightProvider provider = new RuleBasedInsightProvider();

    private SensorReading pm25Reading(double value) {
        return new SensorReading(SensorId.parse("open-meteo:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³", value, Instant.now(), Instant.now(), "open-meteo", Quality.ok());
    }

    @Test
    void anomalyInContextMentionedInSummary() {
        var anomaly = new AnomalyEvent(SensorId.parse("open-meteo:surabaya:pm2_5"),
                "surabaya", Metric.PM2_5, Instant.now(), 300.0, "ZSCORE", 5.2);
        var ctx = new InsightContext(List.of(), List.of(), List.of(anomaly),
                new Location("surabaya", -7.25, 112.75), Instant.now());

        StepVerifier.create(provider.generate(ctx))
                .assertNext(insight -> assertThat(insight.summary()).contains("Anomaly"))
                .verifyComplete();
    }

    @Test
    void goodAqiLabelForLowPm25() {
        assertThat(RuleBasedInsightProvider.aqiLabel(5)).isEqualTo("Good");
        assertThat(RuleBasedInsightProvider.aqiLabel(20)).isEqualTo("Moderate");
        assertThat(RuleBasedInsightProvider.aqiLabel(200)).isEqualTo("Very Unhealthy");
    }

    @Test
    void normalContextProducesNormalRangeMessage() {
        var readings = List.of(pm25Reading(20.0));
        var ctx = new InsightContext(readings, List.of(), List.of(),
                new Location("surabaya", -7.25, 112.75), Instant.now());

        StepVerifier.create(provider.generate(ctx))
                .assertNext(insight -> assertThat(insight.summary()).contains("AQI"))
                .verifyComplete();
    }
}
