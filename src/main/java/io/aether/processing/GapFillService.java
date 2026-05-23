package io.aether.processing;

import io.aether.config.AetherProperties;
import io.aether.domain.*;
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

@Component
public class GapFillService {

    private static final Logger log = LoggerFactory.getLogger(GapFillService.class);

    private final List<Location> locations;
    private final DatabaseClient databaseClient;

    public GapFillService(AetherProperties properties, DatabaseClient databaseClient) {
        this.locations = properties.getLocationList();
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

        return databaseClient.sql(
                "SELECT observed_at, value FROM sensor_reading " +
                "WHERE sensor_id = :sensorId AND observed_at >= :since " +
                "ORDER BY observed_at ASC")
                .bind("sensorId", sensorId)
                .bind("since", since)
                .fetch().all()
                .collectList()
                .flatMap(rows -> insertMissingSlots(rows, sensorId, location, metric));
    }

    private Mono<Long> insertMissingSlots(List<java.util.Map<String, Object>> rows, String sensorId, Location location, Metric metric) {
        if (rows.size() < 2) return Mono.just(0L);
        return Flux.range(0, rows.size() - 1)
                .flatMap(i -> {
                    var prevTime = (OffsetDateTime) rows.get(i).get("observed_at");
                    var nextTime = (OffsetDateTime) rows.get(i + 1).get("observed_at");
                    if (ChronoUnit.MINUTES.between(prevTime, nextTime) <= 90) return Mono.empty();

                    var prevVal = (Double) rows.get(i).get("value");
                    var nextVal = (Double) rows.get(i + 1).get("value");
                    Double imputed = (prevVal != null && nextVal != null) ? (prevVal + nextVal) / 2.0 : null;
                    var missingSlot = prevTime.plusHours(1).truncatedTo(ChronoUnit.HOURS);

                    return databaseClient.sql(
                            "INSERT INTO sensor_reading " +
                            "(sensor_id, location, latitude, longitude, metric, unit, value, " +
                            " observed_at, ingested_at, source, schema_version, quality_status) " +
                            "VALUES " +
                            "(:sensorId, :location, 0, 0, :metric, :unit, :value, " +
                            " :observedAt, now(), 'gap-fill', 1, 'SUSPECT') " +
                            "ON CONFLICT (sensor_id, observed_at) DO NOTHING " +
                            "RETURNING id")
                            .bind("sensorId", sensorId)
                            .bind("location", location.name())
                            .bind("metric", metric.name())
                            .bind("unit", metric.unit())
                            .bindNull("value", Double.class)
                            .bind("observedAt", missingSlot)
                            .fetch().one()
                            .flatMap(row -> databaseClient.sql(
                                    "INSERT INTO reading_flag (reading_id, observed_at, flag) " +
                                    "VALUES (:id, :observedAt, 'IMPUTED') " +
                                    "ON CONFLICT DO NOTHING")
                                    .bind("id", row.get("id"))
                                    .bind("observedAt", missingSlot)
                                    .fetch().rowsUpdated())
                            .defaultIfEmpty(0L);
                })
                .reduce(0L, Long::sum);
    }
}
