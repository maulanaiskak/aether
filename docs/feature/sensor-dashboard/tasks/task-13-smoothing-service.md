# Task 13: Smoothing Service

**Status:** pending
**HLD Reference:** §Functional Requirements FR-07, §Technical Implementation — Processing & Signal (SmoothingService)

## Description

Implement `SmoothingService` which listens to `ReadingValidatedEvent` and computes an EWMA (Exponentially Weighted Moving Average) smoothed value per `sensor_id`. The smoothed value is stored in `sensor_reading.smoothed_value` via an UPDATE. Raw `value` is never overwritten. EWMA state is held in-memory per sensor (a `ConcurrentHashMap`) so no extra DB read is needed per event.

## Acceptance Criteria

- [ ] `@EventListener(ReadingValidatedEvent.class)` subscribes to validated readings
- [ ] EWMA computed per `sensor_id` with configurable `alpha` (default 0.2)
- [ ] First reading for a sensor: `smoothedValue = value` (seed with raw)
- [ ] Subsequent readings: `smoothedValue = alpha * value + (1 - alpha) * prevSmoothed`
- [ ] Null `value` (MISSING_VALUE flag): skip smoothing update for this reading; retain previous smoothed value in memory
- [ ] Smoothed value written via `UPDATE sensor_reading SET smoothed_value = :sv WHERE sensor_id = :sid AND observed_at = :oa`
- [ ] EWMA state map survives the JVM session but is not persisted (acceptable for v1; restart seeds from zero)
- [ ] `SmoothingServiceTest`: inject sequence of readings, assert EWMA converges correctly; assert DB UPDATE called with expected smoothed value

## Dependencies

- **Depends on:** Task 05 (R2DBC DatabaseClient for UPDATE), Task 12 (publishes ReadingValidatedEvent)
- **Blocks:** Task 21 (REST API returns `smoothedValue` in response)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/processing/SmoothingService.java` | Create | EWMA computation + DB update |
| `src/test/java/io/aether/processing/SmoothingServiceTest.java` | Create | Unit test with mock DatabaseClient |

## Implementation Hints

- **EWMA state + update:**
  ```java
  private final ConcurrentHashMap<String, Double> ewmaState = new ConcurrentHashMap<>();

  @EventListener
  public void onReadingValidated(ReadingValidatedEvent event) {
      var reading = event.reading();
      if (reading.value() == null) return;
      var sensorId = reading.sensorId().toString();
      var smoothed = ewmaState.merge(sensorId, reading.value(),
          (prev, curr) -> alpha * curr + (1 - alpha) * prev);
      databaseClient.sql("UPDATE sensor_reading SET smoothed_value = :sv WHERE sensor_id = :sid AND observed_at = :oa")
          .bind("sv", smoothed)
          .bind("sid", sensorId)
          .bind("oa", reading.observedAt())
          .fetch().rowsUpdated()
          .subscribe();
  }
  ```
- **Key consideration:** `@EventListener` by default runs synchronously in the publisher's thread. For async execution, annotate with `@Async` and ensure a task executor is configured. For this volume (a few readings/minute), synchronous is fine.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
