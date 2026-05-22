# Task 16: Anomaly Orchestrator

**Status:** pending
**HLD Reference:** §Technical Implementation — Anomaly Detection (AnomalyOrchestrator), §Sequence Diagram Part 2

## Description

Implement `AnomalyOrchestrator` which listens to `ReadingValidatedEvent`, fetches the recent window for the sensor from DB, runs all three detectors, and if any fires: persists an `anomaly` row and publishes an `AnomalyDetectedEvent`. Only operates on non-REJECTED readings.

## Acceptance Criteria

- [ ] `@EventListener(ReadingValidatedEvent.class)` — skips REJECTED readings
- [ ] Fetches last `${aether.anomaly.window-size:48}` readings for the same `sensor_id` from DB
- [ ] Runs `ZScoreDetector`, `EwmaResidualDetector`, `IqrDetector` on the window + current reading
- [ ] If any detector returns `anomalous=true`: persists `AnomalyEntity` to `anomaly` table; publishes `AnomalyDetectedEvent`
- [ ] One `anomaly` row per detector that fired (a reading can have multiple anomaly rows with different methods)
- [ ] `AnomalyOrchestratorTest` (Testcontainers): inject a sequence of normal readings then a spike; assert `anomaly` table has a row; assert `AnomalyDetectedEvent` was published

## Dependencies

- **Depends on:** Task 05 (AnomalyRepository), Task 12 (ReadingValidatedEvent), Task 15 (detectors)
- **Blocks:** Task 23 (alert publisher reacts to AnomalyDetectedEvent)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/anomaly/AnomalyOrchestrator.java` | Create | Event-driven orchestrator |
| `src/test/java/io/aether/anomaly/AnomalyOrchestratorTest.java` | Create | Testcontainers integration test |

## Implementation Hints

- **Orchestrator body:**
  ```java
  @EventListener
  public void onReading(ReadingValidatedEvent event) {
      var reading = event.reading();
      if (reading.quality().status() == QualityStatus.REJECTED) return;
      anomalyRepository.findTopNBySensorIdOrderByObservedAtDesc(reading.sensorId().toString(), windowSize)
          .collectList()
          .flatMapMany(window -> Flux.fromIterable(detectors)
              .map(d -> d.detect(toReadings(window), reading))
              .filter(AnomalyResult::anomalous))
          .flatMap(result -> anomalyRepository.save(toEntity(reading, result)))
          .doOnNext(saved -> eventPublisher.publishEvent(new AnomalyDetectedEvent(this,
              toAnomalyEvent(reading, saved))))
          .subscribe();
  }
  ```
- **Key consideration:** Window fetch is a DB call per `ReadingValidatedEvent`. With 3 locations × 12 metrics = 36 events/cycle, this is 36 DB queries per hour. Each query fetches 48 rows — modest at this scale. If this were higher frequency, a per-sensor in-memory ring buffer would be better; for v1 the DB approach is simpler and correct.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
