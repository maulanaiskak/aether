# Task 12: Reading Persistence

**Status:** pending
**HLD Reference:** §Functional Requirements FR-05, §Technical Implementation — Processing & Signal (ReadingPersistenceHandler), §Sequence Diagram Part 2

## Description

Subscribe to the `readingSink` from task-10, run validation (task-11) on each reading, upsert the validated reading into `sensor_reading` via R2DBC `DatabaseClient` (not `ReactiveCrudRepository` — custom upsert SQL needed), insert quality flags into `reading_flag`, then publish a `ReadingValidatedEvent` via `ApplicationEventPublisher`. This task wires the processing pipeline together.

## Acceptance Criteria

- [ ] Subscribes to `Sinks.Many<SensorReading>` on application startup
- [ ] Validation applied before any persistence
- [ ] Upsert: `INSERT INTO sensor_reading ... ON CONFLICT (sensor_id, observed_at) DO NOTHING RETURNING id, observed_at` — duplicate hours silently skipped
- [ ] If upsert returns a row (new insert, not conflict): insert flags into `reading_flag` for all non-empty `quality.flags()`
- [ ] If upsert returns empty (conflict/duplicate): skip flag insert; reading was already persisted
- [ ] Publishes `ReadingValidatedEvent(this, validatedReading)` for every reading that passes validation (including duplicates — the event fires even if persistence was skipped, because downstream modules need the reading)
- [ ] `ReadingPersistenceHandlerTest` (Testcontainers Postgres): publishes two identical readings; asserts only one row in DB; asserts `ReadingValidatedEvent` fired twice

## Dependencies

- **Depends on:** Task 05 (R2DBC DatabaseClient), Task 10 (readingSink), Task 11 (validation), Task 04 (events)
- **Blocks:** Task 13, 14, 16, 18 (all listen on `ReadingValidatedEvent`)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/processing/ReadingPersistenceHandler.java` | Create | Validation + upsert + event publish |
| `src/main/java/io/aether/processing/ProcessingPipelineConfig.java` | Create | `@PostConstruct` subscription setup |
| `src/test/java/io/aether/processing/ReadingPersistenceHandlerTest.java` | Create | Testcontainers integration test |

## Implementation Hints

- **R2DBC upsert with DatabaseClient:**
  ```java
  return databaseClient.sql("""
      INSERT INTO sensor_reading
          (sensor_id, location, latitude, longitude, metric, unit, value,
           observed_at, ingested_at, source, schema_version, quality_status)
      VALUES
          (:sensorId, :location, :latitude, :longitude, :metric, :unit, :value,
           :observedAt, :ingestedAt, :source, :schemaVersion, :qualityStatus)
      ON CONFLICT (sensor_id, observed_at) DO NOTHING
      RETURNING id, observed_at
      """)
      .bind("sensorId", reading.sensorId().toString())
      ... // bind all fields
      .fetch().one();  // Mono<Map<String,Object>> — empty on conflict
  ```
- **Pipeline subscription:**
  ```java
  @PostConstruct
  void startPipeline() {
      readingSink.asFlux()
          .flatMap(this::processReading)
          .subscribe(
              r -> {},
              err -> log.error("Pipeline error", err));
  }
  ```
- **Key consideration:** `ReadingValidatedEvent` is published for all readings regardless of whether DB insert happened. Downstream (anomaly, forecast) should process the reading even if it was a duplicate in the DB — the pipeline must not lose events.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
