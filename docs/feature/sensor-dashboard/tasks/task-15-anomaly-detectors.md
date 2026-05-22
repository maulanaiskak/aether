# Task 15: Anomaly Detectors

**Status:** pending
**HLD Reference:** §Technical Implementation — Anomaly Detection (`io.aether.anomaly`), §Functional Requirements FR-09

## Description

Implement the three statistical anomaly detector algorithms as pure functions over a sliding window of readings. All detectors implement a common `AnomalyDetector` interface and are stateless — they receive the window as input and return a score + boolean verdict. No DB access in this task.

## Acceptance Criteria

- [ ] `AnomalyDetector` interface: `AnomalyResult detect(List<SensorReading> window, SensorReading current)`
- [ ] `AnomalyResult` record: `boolean anomalous`, `double score`, `String method`
- [ ] `ZScoreDetector`: compute mean and stddev of `window.value` list; score = `|current.value - mean| / stddev`; anomalous if score > configurable threshold (default 3.0); returns `ZSCORE` method label
- [ ] `EwmaResidualDetector`: compute EWMA over window; residual = `|current.value - ewma|`; normalize by window stddev; anomalous if normalized residual > threshold; returns `EWMA_RESIDUAL` method label
- [ ] `IqrDetector`: compute Q1, Q3, IQR = Q3-Q1; anomalous if `current.value < Q1 - 1.5*IQR` or `> Q3 + 1.5*IQR`; score = distance from fence; returns `IQR` method label
- [ ] All detectors return `AnomalyResult.notAnomalous()` if window has fewer than 5 readings (insufficient data)
- [ ] All detectors skip (return not-anomalous) if `current.quality.status == REJECTED`
- [ ] Unit tests for each detector: edge cases (empty window, constant series, single spike, all-null window)

## Dependencies

- **Depends on:** Task 04 (SensorReading domain type)
- **Blocks:** Task 16 (orchestrator calls detectors)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/anomaly/detector/AnomalyDetector.java` | Create | Interface |
| `src/main/java/io/aether/anomaly/detector/AnomalyResult.java` | Create | Result record |
| `src/main/java/io/aether/anomaly/detector/ZScoreDetector.java` | Create | Z-score implementation |
| `src/main/java/io/aether/anomaly/detector/EwmaResidualDetector.java` | Create | EWMA residual implementation |
| `src/main/java/io/aether/anomaly/detector/IqrDetector.java` | Create | IQR implementation |
| `src/test/java/io/aether/anomaly/detector/ZScoreDetectorTest.java` | Create | Unit tests |
| `src/test/java/io/aether/anomaly/detector/IqrDetectorTest.java` | Create | Unit tests |
| `src/test/java/io/aether/anomaly/detector/EwmaResidualDetectorTest.java` | Create | Unit tests |

## Implementation Hints

- **Z-score:**
  ```java
  var values = window.stream().map(SensorReading::value).filter(Objects::nonNull).toList();
  double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
  double stddev = Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0));
  double score = stddev == 0 ? 0 : Math.abs(current.value() - mean) / stddev;
  ```
- **IQR (use Apache Commons Math or manual percentile):**
  ```java
  var sorted = values.stream().sorted().toList();
  double q1 = percentile(sorted, 25);
  double q3 = percentile(sorted, 75);
  double iqr = q3 - q1;
  boolean anomalous = current.value() < q1 - 1.5 * iqr || current.value() > q3 + 1.5 * iqr;
  ```
  Add `org.apache.commons:commons-math3:3.6.1` to `build.gradle` for percentile computation.
- **Key consideration:** Detectors only operate on non-null values in the window. A window where > 50% of values are null should return not-anomalous (insufficient signal). Log a warning if called with a heavily null window.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
