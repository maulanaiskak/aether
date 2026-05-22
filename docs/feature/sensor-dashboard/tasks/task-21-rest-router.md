# Task 21: REST Router

**Status:** pending
**HLD Reference:** §API Design — REST Endpoints, §Functional Requirements FR-13

## Description

Implement the WebFlux functional router for all REST read endpoints: readings query, readings latest, forecast, forecast metrics, and anomalies. Use `RouterFunction` + `HandlerFunction` pattern (no `@RestController`). All endpoints under `/api/v1/`.

## Acceptance Criteria

- [ ] `GET /api/v1/readings?location=&metric=&from=&to=` — returns paginated `SensorReadingDto` list; validates params (400 if location missing)
- [ ] `GET /api/v1/readings/latest?location=` — returns latest value per sensor for a location
- [ ] `GET /api/v1/forecast?location=&metric=&hours=24` — returns `ForecastPoint` list for next N hours
- [ ] `GET /api/v1/forecast/metrics?location=&model=` — returns `ForecastMetricsDto` list
- [ ] `GET /api/v1/anomalies?location=&metric=&from=&to=` — returns `AnomalyEventDto` list
- [ ] All timestamps in ISO-8601 UTC in responses
- [ ] `Content-Type: application/json` on all REST routes
- [ ] `WebTestClient` tests for each endpoint: 200 happy path, 400 missing required param, 404 unknown location

## Dependencies

- **Depends on:** Task 05 (repositories), Task 18 (forecast data), Task 20 (DTOs)
- **Blocks:** Task 22 (SSE endpoints share router), Task 24 (error handling), Task 25 (FE calls these)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/api/router/ReadingsRouter.java` | Create | RouterFunction for readings endpoints |
| `src/main/java/io/aether/api/handler/ReadingsHandler.java` | Create | HandlerFunction implementations |
| `src/main/java/io/aether/api/router/ForecastRouter.java` | Create | RouterFunction for forecast endpoints |
| `src/main/java/io/aether/api/handler/ForecastHandler.java` | Create | HandlerFunction implementations |
| `src/main/java/io/aether/api/router/AnomalyRouter.java` | Create | RouterFunction for anomaly endpoints |
| `src/main/java/io/aether/api/handler/AnomalyHandler.java` | Create | HandlerFunction implementations |
| `src/main/java/io/aether/api/router/ApiRouterConfig.java` | Create | Combines all RouterFunctions into one bean |
| `src/test/java/io/aether/api/router/ReadingsRouterTest.java` | Create | WebTestClient tests |
| `src/test/java/io/aether/api/router/ForecastRouterTest.java` | Create | WebTestClient tests |

## Implementation Hints

- **Functional router pattern:**
  ```java
  @Bean
  public RouterFunction<ServerResponse> readingsRouter(ReadingsHandler handler) {
      return RouterFunctions.route()
          .GET("/api/v1/readings", handler::queryReadings)
          .GET("/api/v1/readings/latest", handler::latestReadings)
          .build();
  }
  ```
- **Handler with param validation:**
  ```java
  public Mono<ServerResponse> queryReadings(ServerRequest request) {
      var location = request.queryParam("location")
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "location required"));
      // ...
      return ServerResponse.ok().body(readingsFlux, SensorReadingDto.class);
  }
  ```
- **Key consideration:** The `from` and `to` params must be parsed as `Instant` (ISO-8601 with Z suffix). Return `400` with a descriptive error message if they cannot be parsed — do not return a 500.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
