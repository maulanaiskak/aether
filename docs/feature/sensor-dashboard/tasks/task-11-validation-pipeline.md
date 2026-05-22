# Task 11: Validation Pipeline

**Status:** pending
**HLD Reference:** §Functional Requirements FR-06, §Technical Implementation — Processing & Signal (ValidationPipeline)

## Description

Implement the per-datum validation chain that produces a `Quality` verdict for each `SensorReading`. Validation is deterministic and runs in-memory — no DB access. Three validators run in sequence: range, staleness, presence. The result is a new `SensorReading` record with an updated `quality` field. Validators never mutate the input.

## Acceptance Criteria

- [ ] `ReadingValidator` interface: `SensorReading validate(SensorReading reading)` — returns new record with updated quality
- [ ] `RangeValidator`: flags `OUT_OF_RANGE` + status `SUSPECT` if value outside `PhysicalRange` bounds; passes through null values (handled by presence check)
- [ ] `StalenessValidator`: flags `STALE` + status `SUSPECT` if `observedAt` is more than 3 hours behind `Instant.now()`; configurable threshold
- [ ] `PresenceValidator`: flags `MISSING_VALUE` + status `SUSPECT` if `value == null`
- [ ] `ValidationPipeline` chains validators in order: presence → range → staleness; if any produces `REJECTED` (not currently used but reserved), pipeline short-circuits
- [ ] A reading with no flags applied retains `Quality.ok()`
- [ ] `ValidationPipelineTest`: parameterized tests for all boundary conditions (null value, future timestamp, past-limit timestamp, out-of-range, valid)

## Dependencies

- **Depends on:** Task 04 (SensorReading, Quality, QualityFlag domain types), Task 07 (PhysicalRange constants)
- **Blocks:** Task 12 (persistence applies validation before upsert)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/processing/validation/ReadingValidator.java` | Create | Validator interface |
| `src/main/java/io/aether/processing/validation/RangeValidator.java` | Create | Physical range check |
| `src/main/java/io/aether/processing/validation/StalenessValidator.java` | Create | Timestamp freshness check |
| `src/main/java/io/aether/processing/validation/PresenceValidator.java` | Create | Null value check |
| `src/main/java/io/aether/processing/validation/ValidationPipeline.java` | Create | Chains validators in order |
| `src/test/java/io/aether/processing/validation/ValidationPipelineTest.java` | Create | Parameterized unit tests |

## Implementation Hints

- **Chaining validators (functional composition):**
  ```java
  public SensorReading validate(SensorReading reading) {
      return validators.stream()
          .reduce(reading,
              (r, validator) -> validator.validate(r),
              (a, b) -> b);  // combiner unused (sequential)
  }
  ```
- **Merging quality flags across validators:**
  ```java
  // In each validator: add flag to existing set, elevate status if needed
  private SensorReading withFlag(SensorReading r, QualityFlag flag) {
      var existingFlags = new HashSet<>(r.quality().flags());
      existingFlags.add(flag);
      var newStatus = r.quality().status() == QualityStatus.OK ? QualityStatus.SUSPECT : r.quality().status();
      return new SensorReading(..., Quality.of(newStatus, existingFlags));
  }
  ```
- **Key consideration:** Validation is conceptually different from anomaly detection (HLD §FR-06 vs §FR-09). Validation answers "is this datum structurally trustworthy?". Anomaly detection answers "is this datum statistically unusual?". They must not be merged or confused.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
