package io.aether.e2e;

import io.aether.anomaly.entity.AnomalyEntity;
import io.aether.anomaly.repository.AnomalyRepository;
import io.aether.api.dto.AnomalyEventDto;
import io.aether.api.dto.ForecastPointDto;
import io.aether.api.dto.InsightResponseDto;
import io.aether.api.dto.SensorReadingDto;
import io.aether.domain.*;
import io.aether.domain.event.AnomalyDetectedEvent;
import io.aether.domain.event.ReadingValidatedEvent;
import io.aether.forecast.entity.ForecastEntity;
import io.aether.forecast.repository.ForecastRepository;
import io.aether.processing.entity.SensorReadingEntity;
import io.aether.processing.repository.SensorReadingRepository;
import org.junit.jupiter.api.*;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests covering the full HTTP API surface:
 * readings, forecast, anomalies, insight (POST), and SSE streams.
 *
 * Uses in-memory H2 R2DBC to avoid a running database.
 * MQTT and Flyway are disabled; SSE path fires ApplicationEvents directly.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///aether_e2e;DB_CLOSE_DELAY=-1",
                "spring.flyway.enabled=false",
                "aether.mqtt.broker-url=tcp://localhost:11886",
                "aether.ingestion.poll-cron=0 0 0 1 1 ?",
                "aether.insight.provider=rule-based",
                "aether.forecast.ml-service-url=http://localhost:19999",
                "aether.locations[0].name=surabaya",
                "aether.locations[0].lat=-7.25",
                "aether.locations[0].lon=112.75"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SensorDashboardE2ETest {

    @Autowired WebTestClient client;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired SensorReadingRepository readingRepo;
    @Autowired AnomalyRepository anomalyRepo;
    @Autowired ForecastRepository forecastRepo;

    // ── Shared fixtures ────────────────────────────────────────────────────

    static final String LOCATION = "surabaya";
    static final String METRIC   = "PM2_5";

    static SensorReadingEntity readingEntity(double pm25, OffsetDateTime at) {
        return new SensorReadingEntity(
                null, "test:surabaya:pm2_5", LOCATION,
                -7.25, 112.75, METRIC, "µg/m³",
                pm25, pm25 * 0.9,
                at, at, "test", 1, "OK"
        );
    }

    static AnomalyEntity anomalyEntity(double score, OffsetDateTime at) {
        return new AnomalyEntity(
                null, "test:surabaya:pm2_5", LOCATION, METRIC,
                at, 95.0, "zscore", score, at
        );
    }

    static ForecastEntity forecastEntity(OffsetDateTime horizonAt) {
        return new ForecastEntity(
                null, LOCATION, METRIC, horizonAt,
                45.0, 35.0, 55.0, "lgbm", OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    // ── GET /api/v1/readings ───────────────────────────────────────────────

    @Test
    @Order(1)
    void readings_returns_400_when_location_missing() {
        client.get().uri("/api/v1/readings?metric=PM2_5")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(2)
    void readings_returns_400_when_metric_missing() {
        client.get().uri("/api/v1/readings?location=surabaya")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(3)
    void readings_returns_200_with_empty_list_when_no_data() {
        client.get()
                .uri("/api/v1/readings?location={loc}&metric={m}", LOCATION, METRIC)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SensorReadingDto.class)
                .hasSize(0);
    }

    @Test
    @Order(4)
    void readings_returns_saved_rows() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        readingRepo.saveAll(List.of(
                readingEntity(38.5, now.minusHours(2)),
                readingEntity(42.1, now.minusHours(1)),
                readingEntity(50.0, now)
        )).blockLast();

        var result = client.get()
                .uri(u -> u.path("/api/v1/readings")
                        .queryParam("location", LOCATION)
                        .queryParam("metric", METRIC)
                        .queryParam("from", now.minusHours(3).toInstant().toString())
                        .queryParam("to", now.plusHours(1).toInstant().toString())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SensorReadingDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull().hasSize(3);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.location()).isEqualTo(LOCATION);
            assertThat(dto.metric()).isEqualTo(METRIC);
            assertThat(dto.value()).isPositive();
        });
    }

    // ── GET /api/v1/readings/latest ────────────────────────────────────────

    @Test
    @Order(5)
    void latest_readings_returns_200_with_most_recent_reading() {
        // rows already seeded by order-4 test
        client.get()
                .uri("/api/v1/readings/latest?location={loc}", LOCATION)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SensorReadingDto.class)
                .value(dto -> {
                    assertThat(dto.location()).isEqualTo(LOCATION);
                    assertThat(dto.value()).isEqualTo(50.0);
                });
    }

    @Test
    @Order(6)
    void latest_readings_returns_400_when_location_missing() {
        client.get().uri("/api/v1/readings/latest")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── GET /api/v1/anomalies ──────────────────────────────────────────────

    @Test
    @Order(7)
    void anomalies_returns_400_when_params_missing() {
        client.get().uri("/api/v1/anomalies?location=surabaya")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(8)
    void anomalies_returns_saved_events() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        anomalyRepo.saveAll(List.of(
                anomalyEntity(4.2, now.minusHours(5)),
                anomalyEntity(5.7, now.minusHours(1))
        )).blockLast();

        var result = client.get()
                .uri(u -> u.path("/api/v1/anomalies")
                        .queryParam("location", LOCATION)
                        .queryParam("metric", METRIC)
                        .queryParam("from", now.minusHours(6).toInstant().toString())
                        .queryParam("to", now.plusHours(1).toInstant().toString())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AnomalyEventDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull().hasSize(2);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.location()).isEqualTo(LOCATION);
            assertThat(dto.metric()).isEqualTo(METRIC);
            assertThat(dto.score()).isPositive();
        });
    }

    // ── GET /api/v1/forecast ───────────────────────────────────────────────

    @Test
    @Order(9)
    void forecast_returns_400_when_location_missing() {
        client.get().uri("/api/v1/forecast")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(10)
    void forecast_returns_future_points_only() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        forecastRepo.saveAll(List.of(
                forecastEntity(now.minusHours(2)),   // past — must be excluded
                forecastEntity(now.plusHours(1)),
                forecastEntity(now.plusHours(6)),
                forecastEntity(now.plusHours(12))
        )).blockLast();

        var result = client.get()
                .uri("/api/v1/forecast?location={loc}&metric={m}", LOCATION, METRIC)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ForecastPointDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull().hasSize(3);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.predicted()).isEqualTo(45.0);
            assertThat(dto.lowerBound()).isLessThan(dto.predicted());
            assertThat(dto.upperBound()).isGreaterThan(dto.predicted());
            assertThat(dto.horizonAt()).isAfter(Instant.now().minusSeconds(10));
        });
    }

    // ── POST /api/v1/insight ───────────────────────────────────────────────

    @Test
    @Order(11)
    void insight_returns_400_for_unknown_location() {
        client.post().uri("/api/v1/insight")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"location":"unknown","asOf":"2024-01-01T00:00:00Z"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(12)
    void insight_returns_200_with_summary_for_known_location() {
        client.post().uri("/api/v1/insight")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"location":"surabaya","asOf":"%s"}
                        """.formatted(Instant.now()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(InsightResponseDto.class)
                .value(dto -> {
                    assertThat(dto.location()).isEqualTo(LOCATION);
                    assertThat(dto.summary()).isNotBlank();
                    assertThat(dto.provider()).isEqualTo("rule-based");
                });
    }

    // ── GET /api/v1/stream/readings/{location} — SSE ───────────────────────

    @Test
    @Order(13)
    void sse_reading_stream_delivers_event_published_via_application_context() {
        var reading = new SensorReading(
                SensorId.parse("test:surabaya:pm2_5"),
                1, LOCATION, Metric.PM2_5, "µg/m³",
                55.3, Instant.now(), Instant.now(),
                "test", new Quality(QualityStatus.OK, Set.of())
        );

        var flux = client.get()
                .uri("/api/v1/stream/readings/{location}", LOCATION)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<SensorReadingDto>>() {})
                .getResponseBody();

        StepVerifier.create(flux)
                .then(() -> eventPublisher.publishEvent(new ReadingValidatedEvent(this, reading)))
                .expectNextMatches(sse ->
                        sse.data() != null
                        && LOCATION.equals(sse.data().location())
                        && sse.data().value() == 55.3)
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    // ── GET /api/v1/stream/alerts/{location} — SSE ─────────────────────────

    @Test
    @Order(14)
    void sse_alert_stream_delivers_anomaly_event() {
        var anomaly = new AnomalyEvent(
                SensorId.parse("test:surabaya:pm2_5"),
                LOCATION, Metric.PM2_5,
                Instant.now(), 95.0, "zscore", 4.8
        );

        var flux = client.get()
                .uri("/api/v1/stream/alerts/{location}", LOCATION)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<AnomalyEventDto>>() {})
                .getResponseBody();

        StepVerifier.create(flux)
                .then(() -> eventPublisher.publishEvent(new AnomalyDetectedEvent(this, anomaly)))
                .expectNextMatches(sse ->
                        sse.data() != null
                        && LOCATION.equals(sse.data().location())
                        && sse.data().score() == 4.8)
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    // ── Actuator health ────────────────────────────────────────────────────

    @Test
    @Order(15)
    void actuator_health_returns_200() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
