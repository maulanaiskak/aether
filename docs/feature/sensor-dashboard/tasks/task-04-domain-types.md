# Task 04: Domain Types

**Status:** pending
**HLD Reference:** §Technical Implementation — Domain Module (`io.aether.domain`)

## Description

Define all shared value types and Spring `ApplicationEvent` subclasses in the `io.aether.domain` package. This is the shared kernel — no other module may have a circular dependency on any non-domain package. Everything defined here must be pure domain logic with zero infrastructure imports (no Spring Data, no R2DBC, no MQTT). Events are Spring `ApplicationEvent` subclasses so they integrate with `ApplicationEventPublisher` with no extra infrastructure.

## Acceptance Criteria

- [ ] `SensorReading` Java record: `sensorId`, `schemaVersion`, `location`, `metric`, `unit`, `value` (nullable `Double`), `observedAt`, `ingestedAt`, `source`, `quality`
- [ ] `Quality` Java record: `status` (`QualityStatus` enum: OK/SUSPECT/REJECTED), `flags` (`Set<QualityFlag>`)
- [ ] `QualityFlag` enum: `MISSING_VALUE`, `OUT_OF_RANGE`, `STALE`, `IMPUTED`, `ANOMALOUS`
- [ ] `QualityStatus` enum: `OK`, `SUSPECT`, `REJECTED`
- [ ] `Location` Java record: `name`, `lat`, `lon`
- [ ] `Metric` enum: `TEMPERATURE`, `HUMIDITY`, `WIND_SPEED`, `PRESSURE`, `PM2_5`, `PM10`, `O3`, `NO2`, `SO2`, `CO`, `US_AQI`, `EU_AQI`; each carries a `unit` String field
- [ ] `SensorId` value type (record): `source`, `location`, `metric`; `toString()` returns `"source:location:metric"`; static `parse(String)` factory
- [ ] `ReadingValidatedEvent` extends `ApplicationEvent`, carries `SensorReading`
- [ ] `PollCycleCompletedEvent` extends `ApplicationEvent`, carries `Location`
- [ ] `AnomalyDetectedEvent` extends `ApplicationEvent`, carries `AnomalyEvent` record (sensorId, location, metric, observedAt, value, method, score)
- [ ] `ForecastPoint` record: `horizonAt`, `predicted`, `lowerBound`, `upperBound`
- [ ] `InsightContext` record: `recentWindow`, `forecast`, `activeAnomalies`, `location`, `asOf`
- [ ] All types are `Serializable` or records (records are implicitly serializable-safe)
- [ ] Unit tests: `SensorId.parse("open-meteo:surabaya:pm2_5")` round-trips correctly; `Quality` with multiple flags works

## Dependencies

- **Depends on:** Task 01 (project skeleton, correct package root)
- **Blocks:** All feature modules (06–23)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/domain/SensorReading.java` | Create | Canonical reading envelope (record) |
| `src/main/java/io/aether/domain/Quality.java` | Create | Quality verdict record |
| `src/main/java/io/aether/domain/QualityStatus.java` | Create | Enum: OK/SUSPECT/REJECTED |
| `src/main/java/io/aether/domain/QualityFlag.java` | Create | Enum: flag types |
| `src/main/java/io/aether/domain/Location.java` | Create | Location value type (record) |
| `src/main/java/io/aether/domain/Metric.java` | Create | Metric enum with unit field |
| `src/main/java/io/aether/domain/SensorId.java` | Create | Value type with parse() factory |
| `src/main/java/io/aether/domain/AnomalyEvent.java` | Create | Anomaly data record |
| `src/main/java/io/aether/domain/ForecastPoint.java` | Create | Single forecast step record |
| `src/main/java/io/aether/domain/InsightContext.java` | Create | Insight input snapshot record |
| `src/main/java/io/aether/domain/event/ReadingValidatedEvent.java` | Create | ApplicationEvent |
| `src/main/java/io/aether/domain/event/PollCycleCompletedEvent.java` | Create | ApplicationEvent |
| `src/main/java/io/aether/domain/event/AnomalyDetectedEvent.java` | Create | ApplicationEvent |
| `src/test/java/io/aether/domain/SensorIdTest.java` | Create | Unit tests for SensorId parse/toString |
| `src/test/java/io/aether/domain/QualityTest.java` | Create | Unit tests for Quality flag logic |

## Implementation Hints

- **SensorId:**
  ```java
  public record SensorId(String source, String location, String metric) {
      public static SensorId parse(String raw) {
          var parts = raw.split(":", 3);
          if (parts.length != 3) throw new IllegalArgumentException("Invalid SensorId: " + raw);
          return new SensorId(parts[0], parts[1], parts[2]);
      }
      @Override public String toString() { return source + ":" + location + ":" + metric; }
  }
  ```
- **ApplicationEvent pattern:**
  ```java
  public class ReadingValidatedEvent extends ApplicationEvent {
      private final SensorReading reading;
      public ReadingValidatedEvent(Object source, SensorReading reading) {
          super(source);
          this.reading = reading;
      }
      public SensorReading reading() { return reading; }
  }
  ```
- **Metric with unit:**
  ```java
  public enum Metric {
      PM2_5("µg/m³"), PM10("µg/m³"), TEMPERATURE("°C"), HUMIDITY("%"), ...;
      private final String unit;
      Metric(String unit) { this.unit = unit; }
      public String unit() { return unit; }
  }
  ```
- **Key consideration:** No `jakarta.*` or `org.springframework.data.*` imports in domain. Domain types are pure Java — this is enforced by ArchUnit in task-30.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
