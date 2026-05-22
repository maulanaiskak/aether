# Task 14: Gap-Fill Service

**Status:** pending
**HLD Reference:** §Functional Requirements FR-08, §Technical Implementation — Processing & Signal (GapFillService)

## Description

Implement `GapFillService` — a scheduled service that runs every 5 minutes, looks back 2 hours per sensor, detects missing hourly slots, interpolates values linearly, and inserts synthetic rows flagged as `IMPUTED`. The trigger is a `@Scheduled` cron, not per-message — this avoids O(n) scans on the hot ingest path.

## Acceptance Criteria

- [ ] Runs every 5 minutes via `@Scheduled(fixedRate = 300_000)`
- [ ] For each configured location+metric combination, queries the last 2 hours of readings
- [ ] Identifies missing hourly slots (e.g. if `03:00` and `05:00` exist but `04:00` is absent)
- [ ] Interpolates: `imputedValue = (prev.value + next.value) / 2.0`; if either neighbour is null, skip interpolation
- [ ] Inserts synthetic row with `quality_status = SUSPECT`, flag `IMPUTED`, `source = 'gap-fill'`
- [ ] Does NOT overwrite existing rows — upsert with `ON CONFLICT DO NOTHING`
- [ ] `GapFillServiceTest` (Testcontainers): insert two readings 2 hours apart for same sensor; run service; assert the missing middle hour is inserted with IMPUTED flag

## Dependencies

- **Depends on:** Task 05 (SensorReadingRepository), Task 12 (pipeline writes readings)
- **Blocks:** Task 32 (processing integration test validates gap-fill)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/processing/GapFillService.java` | Create | Scheduled gap detection + interpolation |
| `src/test/java/io/aether/processing/GapFillServiceTest.java` | Create | Testcontainers integration test |

## Implementation Hints

- **Gap detection query:**
  ```sql
  SELECT observed_at, value FROM sensor_reading
  WHERE sensor_id = :sensorId
    AND observed_at >= now() - interval '2 hours'
  ORDER BY observed_at ASC
  ```
  Then walk the result set: for consecutive rows where the gap > 1h, the slot in between is missing.
- **Insertion:**
  ```java
  // For each missing slot between prev and next:
  var imputedValue = (prev.value() != null && next.value() != null)
      ? (prev.value() + next.value()) / 2.0 : null;
  // Insert via DatabaseClient with ON CONFLICT DO NOTHING
  ```
- **Key consideration:** This service handles the "IMPUTED" quality flag case. The `IMPUTED` flag is informational — downstream models should know these values are synthetic. The `RuleBasedInsightProvider` (task-19) should note imputed readings in its output.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
