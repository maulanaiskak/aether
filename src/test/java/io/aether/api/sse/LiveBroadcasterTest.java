package io.aether.api.sse;

import io.aether.api.dto.DtoMapper;
import io.aether.domain.*;
import io.aether.domain.event.ReadingValidatedEvent;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

class LiveBroadcasterTest {

    private final LiveBroadcaster broadcaster = new LiveBroadcaster(new DtoMapper());

    @Test
    void readingStreamReceivesEvent() {
        var reading = new SensorReading(
                SensorId.parse("open-meteo:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³",
                25.0, Instant.now(), Instant.now(), "open-meteo", Quality.ok()
        );

        var flux = broadcaster.readingStream("surabaya");
        var verifier = StepVerifier.create(flux)
                .expectNextMatches(sse -> "surabaya".equals(sse.data().location()))
                .thenCancel()
                .verifyLater();

        broadcaster.onReadingValidated(new ReadingValidatedEvent(this, reading));
        verifier.verify(Duration.ofSeconds(1));
    }

    @Test
    void readingStreamFiltersOtherLocations() {
        var reading = new SensorReading(
                SensorId.parse("open-meteo:jakarta:pm2_5"),
                1, "jakarta", Metric.PM2_5, "µg/m³",
                25.0, Instant.now(), Instant.now(), "open-meteo", Quality.ok()
        );

        var flux = broadcaster.readingStream("surabaya");
        StepVerifier.create(flux)
                .then(() -> broadcaster.onReadingValidated(new ReadingValidatedEvent(this, reading)))
                .expectNoEvent(Duration.ofMillis(100))
                .thenCancel()
                .verify(Duration.ofSeconds(1));
    }
}
