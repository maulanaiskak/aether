package io.aether.ingestion.mapper;

import io.aether.domain.*;
import io.aether.ingestion.dto.OpenMeteoAirQualityResponseDto;
import io.aether.ingestion.dto.OpenMeteoWeatherResponseDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SensorReadingMapper {

    public List<SensorReading> mapWeather(OpenMeteoWeatherResponseDto dto, Location location) {
        var hourly = dto.hourly();
        if (hourly == null || hourly.time() == null) return List.of();
        var result = new ArrayList<SensorReading>();
        var times = hourly.time();
        int size = times.size();

        for (int i = 0; i < size; i++) {
            var observedAt = parseTime(times.get(i));
            result.add(reading(location, Metric.TEMPERATURE, safeGet(hourly.temperature2m(), i), observedAt));
            result.add(reading(location, Metric.HUMIDITY, safeGet(hourly.relativeHumidity2m(), i), observedAt));
            result.add(reading(location, Metric.WIND_SPEED, safeGet(hourly.windSpeed10m(), i), observedAt));
            result.add(reading(location, Metric.PRESSURE, safeGet(hourly.surfacePressure(), i), observedAt));
        }
        return result;
    }

    public List<SensorReading> mapAirQuality(OpenMeteoAirQualityResponseDto dto, Location location) {
        var hourly = dto.hourly();
        if (hourly == null || hourly.time() == null) return List.of();
        var result = new ArrayList<SensorReading>();
        var times = hourly.time();
        int size = times.size();

        for (int i = 0; i < size; i++) {
            var observedAt = parseTime(times.get(i));
            result.add(reading(location, Metric.PM2_5, safeGet(hourly.pm25(), i), observedAt));
            result.add(reading(location, Metric.PM10, safeGet(hourly.pm10(), i), observedAt));
            result.add(reading(location, Metric.O3, safeGet(hourly.ozone(), i), observedAt));
            result.add(reading(location, Metric.NO2, safeGet(hourly.nitrogenDioxide(), i), observedAt));
            result.add(reading(location, Metric.SO2, safeGet(hourly.sulphurDioxide(), i), observedAt));
            result.add(reading(location, Metric.CO, safeGet(hourly.carbonMonoxide(), i), observedAt));
            result.add(reading(location, Metric.US_AQI, safeGet(hourly.usAqi(), i), observedAt));
            result.add(reading(location, Metric.EU_AQI, safeGet(hourly.europeanAqi(), i), observedAt));
        }
        return result;
    }

    private SensorReading reading(Location location, Metric metric, Double value, Instant observedAt) {
        var sensorId = new SensorId("open-meteo", location.name(), metric.name().toLowerCase());
        var quality = assess(metric, value);
        return new SensorReading(sensorId, 1, location.name(), metric, metric.unit(),
                value, observedAt, Instant.now(), "open-meteo", quality);
    }

    private Quality assess(Metric metric, Double value) {
        if (value == null) return Quality.suspect(Set.of(QualityFlag.MISSING_VALUE));
        var range = io.aether.processing.validation.PhysicalRange.of(metric);
        if (value < range.min() || value > range.max()) return Quality.suspect(Set.of(QualityFlag.OUT_OF_RANGE));
        return Quality.ok();
    }

    private static Double safeGet(List<Double> list, int index) {
        if (list == null || index >= list.size()) return null;
        return list.get(index);
    }

    private static Instant parseTime(String iso) {
        return LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC);
    }
}
