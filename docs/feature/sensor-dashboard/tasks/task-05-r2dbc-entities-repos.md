# Task 05: R2DBC Entities and Repositories

**Status:** pending
**HLD Reference:** §Data Model — Entity-Relationship Overview, §Technical Implementation — DB driver

## Description

Create R2DBC entity classes and reactive repositories for all five tables (`sensor_reading`, `reading_flag`, `forecast`, `anomaly`, `forecast_metrics`). These are the data access objects — they must not leak into the domain layer. Use Spring Data R2DBC `@Table` entities and `ReactiveCrudRepository` interfaces. The `reading_flag` join table is loaded separately (not an embedded collection) to avoid n+1 patterns.

## Acceptance Criteria

- [ ] `SensorReadingEntity` maps `sensor_reading` table columns; `id` and `observedAt` form the composite key via `@Id` + custom `@Column` mapping
- [ ] `ReadingFlagEntity` maps `reading_flag`; references `readingId` and `observedAt` (no FK enforced at R2DBC level)
- [ ] `ForecastEntity` maps `forecast` table including `lowerBound`, `upperBound`
- [ ] `AnomalyEntity` maps `anomaly` table with `method` as String (ZSCORE/EWMA_RESIDUAL/IQR)
- [ ] `ForecastMetricsEntity` maps `forecast_metrics`
- [ ] `SensorReadingRepository` extends `ReactiveCrudRepository<SensorReadingEntity, Long>` with custom query methods: `findByLocationAndMetricAndObservedAtBetween`, `findLatestByLocation`
- [ ] `ReadingFlagRepository` with `findByReadingIdAndObservedAt`
- [ ] `ForecastRepository` with `findByLocationAndMetricAndHorizonAtAfter`
- [ ] `AnomalyRepository` with `findByLocationAndMetricAndObservedAtBetween`
- [ ] `ForecastMetricsRepository` with `findByLocationAndModel`
- [ ] Compile-time check: no `domain.*` import in any entity or repository class
- [ ] `SensorReadingRepositoryTest` with Testcontainers verifies save + findByLocationAndMetric returns correct row

## Dependencies

- **Depends on:** Task 01 (skeleton), Task 02 (migrations create the tables)
- **Blocks:** Task 12, 13, 14, 16, 18, 19, 21

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/processing/entity/SensorReadingEntity.java` | Create | R2DBC entity for sensor_reading |
| `src/main/java/io/aether/processing/entity/ReadingFlagEntity.java` | Create | R2DBC entity for reading_flag |
| `src/main/java/io/aether/forecast/entity/ForecastEntity.java` | Create | R2DBC entity for forecast |
| `src/main/java/io/aether/anomaly/entity/AnomalyEntity.java` | Create | R2DBC entity for anomaly |
| `src/main/java/io/aether/forecast/entity/ForecastMetricsEntity.java` | Create | R2DBC entity for forecast_metrics |
| `src/main/java/io/aether/processing/repository/SensorReadingRepository.java` | Create | Reactive repository |
| `src/main/java/io/aether/processing/repository/ReadingFlagRepository.java` | Create | Reactive repository |
| `src/main/java/io/aether/forecast/repository/ForecastRepository.java` | Create | Reactive repository |
| `src/main/java/io/aether/anomaly/repository/AnomalyRepository.java` | Create | Reactive repository |
| `src/main/java/io/aether/forecast/repository/ForecastMetricsRepository.java` | Create | Reactive repository |
| `src/test/java/io/aether/processing/repository/SensorReadingRepositoryTest.java` | Create | Testcontainers repo test |

## Implementation Hints

- **Composite key with R2DBC:** Spring Data R2DBC doesn't support composite `@Id` natively. Options: (1) use `id` alone as `@Id` and treat `observedAt` as a regular column; (2) use `DatabaseClient` for custom upsert queries. Recommendation: use `id` as `@Id` for simple CRUD, and write upsert via `DatabaseClient` in `ReadingPersistenceHandler` (task-12).
- **SensorReadingEntity:**
  ```java
  @Table("sensor_reading")
  public record SensorReadingEntity(
      @Id Long id,
      String sensorId,
      String location,
      double latitude,
      double longitude,
      String metric,
      String unit,
      Double value,
      Double smoothedValue,
      OffsetDateTime observedAt,
      OffsetDateTime ingestedAt,
      String source,
      int schemaVersion,
      String qualityStatus
  ) {}
  ```
- **Custom upsert query (used in task-12):**
  ```sql
  INSERT INTO sensor_reading (sensor_id, location, ..., observed_at)
  VALUES (:sensorId, :location, ..., :observedAt)
  ON CONFLICT (sensor_id, observed_at) DO NOTHING
  RETURNING id
  ```
- **Key consideration:** R2DBC `OffsetDateTime` maps to `TIMESTAMPTZ` correctly with the PostgreSQL R2DBC driver. Do not use `LocalDateTime` — it loses timezone context.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
