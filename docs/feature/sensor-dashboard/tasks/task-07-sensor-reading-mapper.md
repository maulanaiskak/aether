# Task 07: SensorReadingMapper

**Status:** pending
**HLD Reference:** §Technical Implementation — Ingestion Gateway, §Data Sources — Canonical envelope

## Description

Implement the mapper that converts raw Open-Meteo JSON DTOs into `SensorReading` domain records. The mapper is the boundary where data quality decisions are made: any missing, null, or out-of-range value becomes a `QualityFlag` on the reading rather than an exception. The mapper must never throw — all errors are encoded as quality flags. One `SensorReading` is emitted per metric per hourly time-slot.

## Acceptance Criteria

- [ ] `SensorReadingMapper.mapWeather(OpenMeteoWeatherResponseDto, Location)` returns `List<SensorReading>` — one per metric per time slot
- [ ] `SensorReadingMapper.mapAirQuality(OpenMeteoAirQualityResponseDto, Location)` same pattern
- [ ] Null value in hourly array → `value = null`, flag `MISSING_VALUE`, status `SUSPECT`
- [ ] Value outside physical range (e.g. PM2.5 < 0 or > 1000) → flag `OUT_OF_RANGE`, status `SUSPECT`
- [ ] `observedAt` parsed from ISO-8601 string in response `time` array; stored as UTC `Instant`
- [ ] `ingestedAt` set to `Instant.now()` at mapping time
- [ ] `sensorId` follows `"open-meteo:{location.name}:{metric.name().toLowerCase()}"` convention
- [ ] `schemaVersion` = 1
- [ ] Physical range constants per metric documented (e.g. `PM2_5: [0, 1000]`, `TEMPERATURE: [-90, 60]`)
- [ ] `SensorReadingMapperTest` unit tests: null value, out-of-range, valid value, missing field in DTO

## Dependencies

- **Depends on:** Task 04 (domain types), Task 06 (DTO classes)
- **Blocks:** Task 09 (scheduler uses mapper output)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/ingestion/mapper/SensorReadingMapper.java` | Create | DTO → SensorReading conversion |
| `src/main/java/io/aether/ingestion/mapper/PhysicalRange.java` | Create | Enum/record holding min/max per Metric |
| `src/test/java/io/aether/ingestion/mapper/SensorReadingMapperTest.java` | Create | Unit tests (parameterized) |

## Implementation Hints

- **Null handling:**
  ```java
  private Quality assess(Metric metric, Double value) {
      if (value == null) return Quality.of(QualityStatus.SUSPECT, Set.of(QualityFlag.MISSING_VALUE));
      var range = PhysicalRange.of(metric);
      if (value < range.min() || value > range.max())
          return Quality.of(QualityStatus.SUSPECT, Set.of(QualityFlag.OUT_OF_RANGE));
      return Quality.ok();
  }
  ```
- **Physical range constants:**
  ```java
  public enum PhysicalRange {
      PM2_5(0, 1000), PM10(0, 2000), TEMPERATURE(-90, 60),
      HUMIDITY(0, 100), WIND_SPEED(0, 200), PRESSURE(870, 1085),
      O3(0, 1000), NO2(0, 2000), SO2(0, 2000), CO(0, 100_000),
      US_AQI(0, 500), EU_AQI(0, 500);
      ...
  }
  ```
- **Key consideration:** Open-Meteo returns one array of timestamps and one array per variable. The mapper must zip `time[i]` with `variable[i]` — they are parallel arrays, not named objects. A null entry in the variable array (not the whole array) means that specific hour had no data.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
