package io.aether.api;

import io.aether.api.dto.SensorReadingDto;
import io.aether.domain.Metric;
import io.aether.domain.Quality;
import io.aether.domain.QualityStatus;
import io.aether.domain.SensorId;
import io.aether.domain.SensorReading;
import io.aether.domain.event.ReadingValidatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///aether_test",
                "spring.flyway.enabled=false",
                "aether.mqtt.broker-url=tcp://localhost:11883",
                "aether.mqtt.client-id-prefix=test",
                "aether.locations[0].name=Surabaya",
                "aether.locations[0].lat=-7.25",
                "aether.locations[0].lon=112.75",
                "aether.insight.provider=rule-based",
                "aether.forecast.ml-service-url=http://localhost:19999"
        })
class SseIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Test
    void reading_event_reaches_sse_stream() {
        var reading = sampleReading("surabaya", Metric.PM2_5, 38.4);

        var result = webTestClient
                .get()
                .uri("/api/v1/stream/readings/surabaya")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<SensorReadingDto>>() {});

        StepVerifier.create(result.getResponseBody())
                .then(() -> eventPublisher.publishEvent(new ReadingValidatedEvent(this, reading)))
                .expectNextMatches(sse -> sse.data() != null && "surabaya".equals(sse.data().location()))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    private static SensorReading sampleReading(String location, Metric metric, double value) {
        return new SensorReading(
                SensorId.parse("test:" + location + ":" + metric.name().toLowerCase()),
                1,
                location,
                metric,
                metric.unit(),
                value,
                Instant.now(),
                Instant.now(),
                "test",
                new Quality(QualityStatus.OK, Set.of())
        );
    }
}
