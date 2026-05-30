package io.aether.ingestion;

import io.aether.config.AetherProperties;
import io.aether.domain.Location;
import io.aether.domain.SensorReading;
import io.aether.ingestion.dto.OpenMeteoAirQualityResponseDto;
import io.aether.ingestion.dto.OpenMeteoWeatherResponseDto;
import io.aether.ingestion.mapper.SensorReadingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class HistoricalBackfillService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalBackfillService.class);

    private final List<Location> locations;
    private final WebClient archiveWebClient;
    private final SensorReadingMapper mapper;
    private final Sinks.Many<SensorReading> readingSink;
    private final DatabaseClient databaseClient;

    public HistoricalBackfillService(
            AetherProperties properties,
            WebClient.Builder builder,
            SensorReadingMapper mapper,
            Sinks.Many<SensorReading> readingSink,
            DatabaseClient databaseClient) {
        this.locations = properties.getLocationList();
        this.archiveWebClient = builder.baseUrl("https://archive-api.open-meteo.com").build();
        this.mapper = mapper;
        this.readingSink = readingSink;
        this.databaseClient = databaseClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillIfEmpty() {
        databaseClient.sql("SELECT COUNT(*) FROM sensor_reading")
                .fetch().one()
                .map(row -> ((Number) row.get("count")).longValue())
                .flatMap(count -> {
                    if (count > 100) {
                        log.info("Backfill skipped — {} rows already present", count);
                        return Mono.empty();
                    }
                    log.info("Starting 1-year historical backfill for {} locations", locations.size());
                    return Flux.fromIterable(locations)
                            .concatMap(this::backfillLocation)
                            .then();
                })
                .subscribe(
                        null,
                        err -> log.error("Backfill failed: {}", err.getMessage())
                );
    }

    private Mono<Void> backfillLocation(Location location) {
        var endDate = LocalDate.now().minusDays(1);
        var startDate = endDate.minusYears(1);
        var start = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        var end = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        log.info("Backfilling {} from {} to {}", location.name(), start, end);

        var weatherMono = archiveWebClient.get()
                .uri(u -> u.path("/v1/archive")
                        .queryParam("latitude", location.lat())
                        .queryParam("longitude", location.lon())
                        .queryParam("start_date", start)
                        .queryParam("end_date", end)
                        .queryParam("hourly", "temperature_2m,relative_humidity_2m,wind_speed_10m,surface_pressure")
                        .queryParam("timezone", "UTC")
                        .build())
                .retrieve()
                .bodyToMono(OpenMeteoWeatherResponseDto.class)
                .onErrorResume(e -> { log.warn("Archive weather failed for {}: {}", location.name(), e.getMessage()); return Mono.just(new OpenMeteoWeatherResponseDto(0, 0, "UTC", null)); });

        var aqMono = archiveWebClient.get()
                .uri(u -> u.path("/v1/air-quality")
                        .queryParam("latitude", location.lat())
                        .queryParam("longitude", location.lon())
                        .queryParam("start_date", start)
                        .queryParam("end_date", end)
                        .queryParam("hourly", "pm2_5,pm10,ozone,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide,us_aqi,european_aqi")
                        .queryParam("timezone", "UTC")
                        .build())
                .retrieve()
                .bodyToMono(OpenMeteoAirQualityResponseDto.class)
                .onErrorResume(e -> { log.warn("Archive AQ failed for {}: {}", location.name(), e.getMessage()); return Mono.just(new OpenMeteoAirQualityResponseDto(0, 0, "UTC", null)); });

        return Mono.zip(weatherMono, aqMono)
                .flatMapMany(tuple -> {
                    var readings = new ArrayList<>(mapper.mapWeather(tuple.getT1(), location));
                    readings.addAll(mapper.mapAirQuality(tuple.getT2(), location));
                    return Flux.fromIterable(readings);
                })
                .doOnNext(readingSink::tryEmitNext)
                .count()
                .doOnNext(n -> log.info("Backfill emitted {} readings for {}", n, location.name()))
                .then();
    }
}
