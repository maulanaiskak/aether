# Task 18: Forecast Client + Orchestrator

**Status:** pending
**HLD Reference:** §Technical Implementation — ML Forecast Service (ForecastClient, ForecastOrchestrator), §Sequence Diagram Forecast Flow

## Description

Implement `ForecastClient` (WebClient wrapper for the Python sidecar) and `ForecastOrchestrator` (triggered by `PollCycleCompletedEvent` — once per location per poll cycle). The orchestrator fetches the recent window from DB, calls the sidecar, upserts forecast rows, and evaluates MAE/RMSE vs a naive persistence baseline.

## Acceptance Criteria

- [ ] `ForecastClient.forecast(Location)` calls `POST http://ml-service:8000/forecast` with the last 72 h of PM2.5 readings + weather features; returns `Mono<ForecastResponse>`
- [ ] `ForecastOrchestrator` listens on `PollCycleCompletedEvent` (fires once per location per poll cycle)
- [ ] For each location: fetch window → call sidecar → upsert `forecast` rows (`ON CONFLICT (location, metric, horizon_at) DO UPDATE SET predicted = EXCLUDED.predicted, ...`)
- [ ] After upsert: compute MAE and RMSE of the previous forecast run against actual readings that are now available; insert into `forecast_metrics`
- [ ] `ForecastClientTest` with WireMock: verifies correct request body, response mapped to domain types
- [ ] `ForecastOrchestratorTest` with Testcontainers: event fired → forecast rows upserted

## Dependencies

- **Depends on:** Task 05 (ForecastRepository), Task 12 (readings in DB), Task 17 (sidecar contract)
- **Blocks:** Task 21 (REST API serves /api/v1/forecast), Task 28 (FE displays forecast band)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/forecast/ForecastClient.java` | Create | HTTP client to Python sidecar |
| `src/main/java/io/aether/forecast/dto/ForecastRequestDto.java` | Create | Request shape |
| `src/main/java/io/aether/forecast/dto/ForecastResponseDto.java` | Create | Response shape |
| `src/main/java/io/aether/forecast/ForecastOrchestrator.java` | Create | Event-driven forecast trigger |
| `src/main/java/io/aether/forecast/ForecastMetricsService.java` | Create | MAE/RMSE vs baseline |
| `src/test/java/io/aether/forecast/ForecastClientTest.java` | Create | WireMock test |
| `src/test/java/io/aether/forecast/ForecastOrchestratorTest.java` | Create | Testcontainers test |

## Implementation Hints

- **Persistence baseline:** "prediction for hour H = actual value at hour H-1" (persistence). MAE = mean of `|actual[H] - forecast[H]|` across the held-out window.
- **Forecast upsert SQL:**
  ```sql
  INSERT INTO forecast (location, metric, horizon_at, predicted, lower_bound, upper_bound, model)
  VALUES (:location, 'PM2_5', :horizonAt, :predicted, :lower, :upper, :model)
  ON CONFLICT (location, metric, horizon_at)
  DO UPDATE SET predicted = EXCLUDED.predicted,
               lower_bound = EXCLUDED.lower_bound,
               upper_bound = EXCLUDED.upper_bound,
               model = EXCLUDED.model,
               created_at = now()
  ```
- **Key consideration:** The sidecar may be slow at startup (model loading). `ForecastClient` should have a 30-second timeout and return `Mono.empty()` on timeout, so a slow sidecar doesn't block the event handler thread.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
