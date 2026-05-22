# Task 02: Flyway Migrations

**Status:** pending
**HLD Reference:** §Data Model — DDL (Flyway migrations)

## Description

Write all five Flyway SQL migrations that establish the complete database schema. Migrations run automatically on aerator startup via the `FlywayConfig` JDBC DataSource (task-01). The TimescaleDB hypertable migration is order-sensitive: the extension must exist before the hypertable DDL.

## Acceptance Criteria

- [ ] `V1__enable_timescaledb.sql` — `CREATE EXTENSION IF NOT EXISTS timescaledb`
- [ ] `V2__create_sensor_reading.sql` — hypertable with composite PK `(id, observed_at)`, `UNIQUE (sensor_id, observed_at)`, index on `(location, metric, observed_at DESC)`, no `TEXT[]` column
- [ ] `V3__create_reading_flag.sql` — join table `reading_flag(reading_id, observed_at, flag)` with FK to `sensor_reading`, PK `(reading_id, observed_at, flag)`, index on `flag`
- [ ] `V4__create_forecast.sql` — `forecast` table with `UNIQUE (location, metric, horizon_at)` constraint, index on `(location, metric, horizon_at DESC)`
- [ ] `V5__create_anomaly.sql` — `anomaly` table, index on `(location, metric, observed_at DESC)`
- [ ] `V6__create_forecast_metrics.sql` — `forecast_metrics` table, index on `(location, model, evaluated_at DESC)`
- [ ] `./gradlew test` with a Testcontainers Postgres image runs migrations successfully (add a `@FlywayTest` smoke test or verify via `FlywayAutoConfiguration` test)
- [ ] Re-running migrations produces no errors (`IF NOT EXISTS` guards and Flyway's checksum validation pass)

## Dependencies

- **Depends on:** Task 01 (project skeleton, FlywayConfig bean)
- **Blocks:** Task 03, Task 05

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/resources/db/migration/V1__enable_timescaledb.sql` | Create | TimescaleDB extension |
| `src/main/resources/db/migration/V2__create_sensor_reading.sql` | Create | Hypertable for readings |
| `src/main/resources/db/migration/V3__create_reading_flag.sql` | Create | Quality flags join table |
| `src/main/resources/db/migration/V4__create_forecast.sql` | Create | Forecast storage |
| `src/main/resources/db/migration/V5__create_anomaly.sql` | Create | Anomaly events |
| `src/main/resources/db/migration/V6__create_forecast_metrics.sql` | Create | Model evaluation metrics |
| `src/test/java/io/aether/MigrationSmokeTest.java` | Create | Verifies all migrations apply cleanly |

## Implementation Hints

- **V2 — sensor_reading (no TEXT[]):**
  ```sql
  CREATE TABLE sensor_reading (
      id             BIGINT GENERATED ALWAYS AS IDENTITY,
      sensor_id      TEXT             NOT NULL,
      location       TEXT             NOT NULL,
      latitude       DOUBLE PRECISION NOT NULL,
      longitude      DOUBLE PRECISION NOT NULL,
      metric         TEXT             NOT NULL,
      unit           TEXT             NOT NULL,
      value          DOUBLE PRECISION,
      smoothed_value DOUBLE PRECISION,
      observed_at    TIMESTAMPTZ      NOT NULL,
      ingested_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
      source         TEXT             NOT NULL DEFAULT 'open-meteo',
      schema_version INT              NOT NULL DEFAULT 1,
      quality_status TEXT             NOT NULL DEFAULT 'OK',
      PRIMARY KEY (id, observed_at),
      UNIQUE (sensor_id, observed_at)
  );
  SELECT create_hypertable('sensor_reading', 'observed_at', if_not_exists => TRUE);
  CREATE INDEX idx_reading_loc_metric_time ON sensor_reading (location, metric, observed_at DESC);
  ```
- **V3 — reading_flag:**
  ```sql
  CREATE TABLE reading_flag (
      reading_id  BIGINT      NOT NULL,
      observed_at TIMESTAMPTZ NOT NULL,
      flag        TEXT        NOT NULL,
      PRIMARY KEY (reading_id, observed_at, flag),
      FOREIGN KEY (reading_id, observed_at) REFERENCES sensor_reading (id, observed_at) ON DELETE CASCADE
  );
  CREATE INDEX idx_reading_flag_flag ON reading_flag (flag);
  ```
- **Key consideration:** TimescaleDB Docker image must be `timescale/timescaledb:2.17.2-pg16` — plain `postgres` image does not have TimescaleDB extension and V1 migration will fail. For the migration smoke test, use `timescale/timescaledb:2.17.2-pg16` as the Testcontainers image.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
