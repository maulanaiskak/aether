# Task 32: Processing Integration Test

**Status:** pending
**HLD Reference:** §Testing Strategy — Processing integration, §Milestones M2 acceptance

## Description

Integration tests for the processing pipeline: idempotent upsert, gap-fill, and smoothing. Uses Testcontainers with the TimescaleDB image. Verifies the core data-quality claims of the project.

## Acceptance Criteria

- [ ] **Upsert idempotency test**: publish the same `SensorReading` twice to `readingSink`; assert only one row in `sensor_reading` with correct `sensor_id + observed_at`; assert `ReadingValidatedEvent` fired twice (events fire even on duplicate)
- [ ] **Gap-fill test**: insert readings at `T` and `T+2h` for the same sensor; run `GapFillService` manually; assert a row exists at `T+1h` with `IMPUTED` flag and `source = gap-fill`
- [ ] **Gap-fill idempotency**: run gap-fill twice; assert still only one imputed row
- [ ] **Smoothing test**: inject a sequence of 5 readings with known values; assert `smoothed_value` in DB is within 0.01 of hand-computed EWMA
- [ ] **Validation flags test**: inject a reading with `value = -5.0` for PM2.5 (negative, out of range); assert `quality_status = SUSPECT` and `reading_flag` table has an `OUT_OF_RANGE` row for that reading

## Dependencies

- **Depends on:** Task 12 (persistence), Task 13 (smoothing), Task 14 (gap-fill)
- **Blocks:** Task 34 (CI)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/test/java/io/aether/processing/UpsertIdempotencyTest.java` | Create | Duplicate insert assertion |
| `src/test/java/io/aether/processing/GapFillIntegrationTest.java` | Create | Gap detection and fill |
| `src/test/java/io/aether/processing/SmoothingIntegrationTest.java` | Create | EWMA in DB |
| `src/test/java/io/aether/processing/ValidationFlagsIntegrationTest.java` | Create | Flags persisted to reading_flag |

## Implementation Hints

- **Shared Testcontainers config:** Use a `@TestConfiguration` base class or JUnit 5 `@ExtendWith` with the TimescaleDB container to avoid starting a container per test class.
- **TimescaleDB in Testcontainers:**
  ```java
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("timescale/timescaledb:2.17.2-pg16")
      .withDatabaseName("aether_test")
      .withUsername("aether")
      .withPassword("aether");
  ```
- **Key consideration:** These tests are the primary evidence for the project's data-quality claims. Write them before showcasing — a reviewer who clones the repo and runs `./gradlew test` should see these pass clearly.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
