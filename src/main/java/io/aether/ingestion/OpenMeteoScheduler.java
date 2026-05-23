package io.aether.ingestion;

import io.aether.config.AetherProperties;
import io.aether.domain.Location;
import io.aether.domain.event.PollCycleCompletedEvent;
import io.aether.ingestion.client.OpenMeteoAirQualityClient;
import io.aether.ingestion.client.OpenMeteoWeatherClient;
import io.aether.ingestion.dto.OpenMeteoAirQualityResponseDto;
import io.aether.ingestion.dto.OpenMeteoWeatherResponseDto;
import io.aether.ingestion.mapper.SensorReadingMapper;
import io.aether.ingestion.mqtt.MqttPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class OpenMeteoScheduler {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoScheduler.class);

    private final List<Location> locations;
    private final OpenMeteoWeatherClient weatherClient;
    private final OpenMeteoAirQualityClient aqClient;
    private final SensorReadingMapper mapper;
    private final MqttPublisher mqttPublisher;
    private final ApplicationEventPublisher eventPublisher;

    public OpenMeteoScheduler(
            AetherProperties properties,
            OpenMeteoWeatherClient weatherClient,
            OpenMeteoAirQualityClient aqClient,
            SensorReadingMapper mapper,
            MqttPublisher mqttPublisher,
            ApplicationEventPublisher eventPublisher) {
        this.locations = properties.getLocationList();
        this.weatherClient = weatherClient;
        this.aqClient = aqClient;
        this.mapper = mapper;
        this.mqttPublisher = mqttPublisher;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void pollOnStartup() {
        poll();
    }

    @Scheduled(cron = "${aether.ingestion.poll-cron}")
    public void poll() {
        for (Location location : locations) {
            try {
                Mono.zip(
                        weatherClient.fetch(location).defaultIfEmpty(emptyWeather()),
                        aqClient.fetch(location).defaultIfEmpty(emptyAq())
                )
                .flatMapMany(tuple -> {
                    var readings = new java.util.ArrayList<>(mapper.mapWeather(tuple.getT1(), location));
                    readings.addAll(mapper.mapAirQuality(tuple.getT2(), location));
                    return Flux.fromIterable(readings);
                })
                .doOnNext(mqttPublisher::publish)
                .then(Mono.fromRunnable(() ->
                        eventPublisher.publishEvent(new PollCycleCompletedEvent(this, location))))
                .block();
            } catch (Exception e) {
                log.error("Poll failed for {}: {}", location.name(), e.getMessage());
                eventPublisher.publishEvent(new PollCycleCompletedEvent(this, location));
            }
        }
    }

    private OpenMeteoWeatherResponseDto emptyWeather() {
        return new OpenMeteoWeatherResponseDto(0, 0, "UTC", null);
    }

    private OpenMeteoAirQualityResponseDto emptyAq() {
        return new OpenMeteoAirQualityResponseDto(0, 0, "UTC", null);
    }
}
