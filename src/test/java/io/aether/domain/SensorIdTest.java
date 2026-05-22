package io.aether.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorIdTest {

    @Test
    void parseRoundTrip() {
        var raw = "open-meteo:surabaya:pm2_5";
        var id = SensorId.parse(raw);
        assertThat(id.source()).isEqualTo("open-meteo");
        assertThat(id.location()).isEqualTo("surabaya");
        assertThat(id.metric()).isEqualTo("pm2_5");
        assertThat(id.toString()).isEqualTo(raw);
    }

    @Test
    void parseInvalidThrows() {
        assertThatThrownBy(() -> SensorId.parse("bad-input"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
