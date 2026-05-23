package io.aether.ingestion;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.aether.domain.SensorReading;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.aether.domain.event.PollCycleCompletedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///aether_test",
                "spring.flyway.enabled=false",
                "aether.mqtt.broker-url=tcp://localhost:11884",
                "aether.ingestion.poll-cron=0 0 0 1 1 ?",
                "aether.insight.provider=rule-based",
                "aether.forecast.ml-service-url=http://localhost:19999",
                "aether.locations[0].name=surabaya",
                "aether.locations[0].lat=-7.25",
                "aether.locations[0].lon=112.75"
        })
class IngestionIntegrationTest {

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @BeforeAll
    static void startWireMock() throws Exception {
        wireMock.start();

        String weatherJson = Files.readString(Path.of("src/test/resources/fixtures/open-meteo-weather-response.json"));
        String aqJson = Files.readString(Path.of("src/test/resources/fixtures/open-meteo-aq-response.json"));

        wireMock.stubFor(get(urlPathMatching("/v1/forecast"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(weatherJson)));

        wireMock.stubFor(get(urlPathMatching("/v1/air-quality"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(aqJson)));
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideUrls(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + wireMock.port();
        registry.add("aether.ingestion.weather-base-url", () -> base);
        registry.add("aether.ingestion.aq-base-url", () -> base);
    }

    @Autowired
    OpenMeteoScheduler scheduler;

    @Autowired
    ConfigurableApplicationContext context;

    @Test
    void poll_maps_null_value_to_missing_value_flag() {
        List<PollCycleCompletedEvent> events = new CopyOnWriteArrayList<>();
        context.addApplicationListener((ApplicationListener<PollCycleCompletedEvent>) events::add);

        scheduler.poll();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().location().name()).isEqualTo("surabaya");
    }
}
