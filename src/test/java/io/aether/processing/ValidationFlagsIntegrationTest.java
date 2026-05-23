package io.aether.processing;

import io.aether.domain.*;
import io.aether.processing.validation.ValidationPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///aether_test",
                "spring.flyway.enabled=false",
                "aether.mqtt.broker-url=tcp://localhost:11885",
                "aether.insight.provider=rule-based",
                "aether.forecast.ml-service-url=http://localhost:19999",
                "aether.locations[0].name=surabaya",
                "aether.locations[0].lat=-7.25",
                "aether.locations[0].lon=112.75"
        })
class ValidationFlagsIntegrationTest {

    @Autowired
    ValidationPipeline validationPipeline;

    @Test
    void negative_pm25_value_produces_out_of_range_flag() {
        var reading = new SensorReading(
                SensorId.parse("test:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³",
                -5.0, Instant.now(), Instant.now(),
                "test", new Quality(QualityStatus.OK, Set.of())
        );

        var validated = validationPipeline.validate(reading);

        assertThat(validated.quality().status()).isEqualTo(QualityStatus.SUSPECT);
        assertThat(validated.quality().flags()).contains(QualityFlag.OUT_OF_RANGE);
    }

    @Test
    void null_value_produces_missing_value_flag() {
        var reading = new SensorReading(
                SensorId.parse("test:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³",
                null, Instant.now(), Instant.now(),
                "test", new Quality(QualityStatus.OK, Set.of())
        );

        var validated = validationPipeline.validate(reading);

        assertThat(validated.quality().status()).isEqualTo(QualityStatus.SUSPECT);
        assertThat(validated.quality().flags()).contains(QualityFlag.MISSING_VALUE);
    }

    @Test
    void valid_pm25_value_is_ok() {
        var reading = new SensorReading(
                SensorId.parse("test:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³",
                35.5, Instant.now(), Instant.now(),
                "test", new Quality(QualityStatus.OK, Set.of())
        );

        var validated = validationPipeline.validate(reading);

        assertThat(validated.quality().status()).isEqualTo(QualityStatus.OK);
        assertThat(validated.quality().flags()).isEmpty();
    }
}
