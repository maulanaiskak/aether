package io.aether.processing;

import io.aether.config.AetherProperties;
import io.aether.domain.*;
import io.aether.processing.repository.SensorReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class GapFillService {

    private static final Logger log = LoggerFactory.getLogger(GapFillService.class);

    private final List<Location> locations;
    private final SensorReadingRepository repository;
    private final DatabaseClient databaseClient;

    public GapFillService(AetherProperties properties,
                          SensorReadingRepository repository,
                          DatabaseClient databaseClient) {
        this.locations = properties.getLocationList();
        this.repository = repository;
        this.databaseClient = databaseClient;
    }

    @Scheduled(fixedRate = 300_000)
    public void fillGaps() {
        for (var location : locations) {
            for (var metric : Metric.values()) {
                fillGapsFor(location, metric).subscribe(
                        count -> { if (count > 0) log.info("Gap-filled {} rows for {}/{}", count, location.name(), metric.name()); },
                        err -> log.error("Gap fill failed for {}/{}: {}", location.name(), metric.name(), err.getMessage())
                );
            }
        }
    }

    Mono<Long> fillGapsFor(Location location, Metric metric) {
        var sensorId = "open-meteo:" + location.name() + ":" + metric.name().toLowerCase();
        var since = OffsetDateTime.ofInstant(Instant.now().minus(2, ChronoUnit.HOURS), ZoneOffset.UTC);

        return repository.findBySensorIdAndObservedAtGreaterThanEqualOrderByObservedAtAsc(sensorId, since)
                .collectList()
                .flatMap(rows -> insertMissingSlots(rows, sensorId, location, metric));
    }

    private Mono<Long> insertMissingSlots(
            List<io.aether.processing.entity.SensorReadingEntity> rows,
            String sensorId, Location location, Metric metric) {
        if (rows.size() < 2) return Mono.just(0L);
        return Flux.range(0, rows.size() - 1)
                .flatMap(i -> {
                    var prevTime = rows.get(i).observedAt();
                    var nextTime = rows.get(i + 1).observedAt();
                    if (ChronoUnit.MINUTES.between(prevTime, nextTime) <= 90) return Mono.empty();

                    var missingSlot = prevTime.plusHours(1).truncatedTo(ChronoUnit.HOURS);

                    return databaseClient.sql(
                            "INSERT INTO sensor_reading " +
                            "(sensor_id, location, latitude, longitude, metric, unit, value, " +
                            " observed_at, ingested_at, source, schema_version, quality_status) " +
                            "VALUES ($1, $2, 0, 0, $3, $4, NULL, $5, now(), 'gap-fill', 1, 'SUSPECT') " +
                            "ON CONFLICT (sensor_id, observed_at) DO NOTHING " +
                            "RETURNING id")
                            .bind(0, sensorId)
                            .bind(1, location.name())
                            .bind(2, metric.name())
                            .bind(3, metric.unit())
                            .bind(4, missingSlot)
                            .fetch().one()
                            .flatMap(row -> databaseClient.sql(
                                    "INSERT INTO reading_flag (reading_id, observed_at, flag) " +
                                    "VALUES ($1, $2, 'IMPUTED') ON CONFLICT DO NOTHING")
                                    .bind(0, row.get("id"))
                                    .bind(1, missingSlot)
                                    .fetch().rowsUpdated())
                            .defaultIfEmpty(0L);
                })
                .reduce(0L, Long::sum);
    }
}
