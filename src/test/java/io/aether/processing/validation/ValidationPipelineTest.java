package io.aether.processing.validation;

import io.aether.domain.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationPipelineTest {

    private final ValidationPipeline pipeline = new ValidationPipeline(
            new PresenceValidator(),
            new RangeValidator(),
            new StalenessValidator(Duration.ofHours(3))
    );

    static SensorReading reading(Double value, Instant observedAt) {
        return new SensorReading(
                SensorId.parse("open-meteo:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³",
                value, observedAt, Instant.now(), "open-meteo", Quality.ok()
        );
    }

    static Stream<Arguments> cases() {
        var now = Instant.now();
        return Stream.of(
                Arguments.of("valid", 25.0, now, QualityStatus.OK, Set.of()),
                Arguments.of("null-value", null, now, QualityStatus.SUSPECT, Set.of(QualityFlag.MISSING_VALUE)),
                Arguments.of("out-of-range", 9999.0, now, QualityStatus.SUSPECT, Set.of(QualityFlag.OUT_OF_RANGE)),
                Arguments.of("stale", 25.0, now.minus(Duration.ofHours(5)), QualityStatus.SUSPECT, Set.of(QualityFlag.STALE))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void validate(String name, Double value, Instant observedAt, QualityStatus expectedStatus, Set<QualityFlag> expectedFlags) {
        var result = pipeline.validate(reading(value, observedAt));
        assertThat(result.quality().status()).isEqualTo(expectedStatus);
        assertThat(result.quality().flags()).containsExactlyInAnyOrderElementsOf(expectedFlags);
    }
}
