# Task 23: Insight Endpoint + MQTT Alert Publisher

**Status:** pending
**HLD Reference:** §API Design — POST /api/v1/insight, §Technical Implementation — API Layer (MQTT ownership), §Domain Types (AnomalyDetectedEvent)

## Description

Wire two things in the `api` module: (1) the `POST /api/v1/insight` endpoint that builds `InsightContext` and calls `InsightProvider`; (2) the `AnomalyDetectedEvent` listener that publishes to `alerts/{loc}/{metric}` MQTT topic via the `AlertPublisher` (wired in task-08). These are combined because both involve the `api` module reacting to domain events.

## Acceptance Criteria

- [ ] `POST /api/v1/insight` body: `{ "location": "surabaya", "asOf": "2026-05-22T06:00:00Z" }`
- [ ] Handler builds `InsightContext` via `InsightContextBuilder` (fetches recent window, forecast, anomalies from DB)
- [ ] Calls `InsightProvider.generate(ctx)` and returns `InsightResponseDto`
- [ ] Response: `{ "location", "asOf", "summary", "provider" }` with `200 OK`
- [ ] Returns `400` if location not in configured list; `503` if context fetch fails
- [ ] `AnomalyAlertRelay` `@EventListener(AnomalyDetectedEvent.class)` → calls `alertPublisher.publish(AnomalyEvent)` to `alerts/{location}/{metric}` MQTT topic
- [ ] `InsightEndpointTest` (WebTestClient): POST with valid location; assert 200 and non-empty summary
- [ ] `AnomalyAlertRelayTest`: publish `AnomalyDetectedEvent`; verify MQTT outbound channel received a message with correct topic

## Dependencies

- **Depends on:** Task 16 (AnomalyDetectedEvent source), Task 19 (InsightProvider), Task 20 (alert publisher from task-08's AlertPublisher)
- **Blocks:** Task 24 (error handling), Task 29 (FE insight panel)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/api/router/InsightRouter.java` | Create | RouterFunction for POST /api/v1/insight |
| `src/main/java/io/aether/api/handler/InsightHandler.java` | Create | Builds context, calls provider, returns response |
| `src/main/java/io/aether/api/dto/InsightResponseDto.java` | Create | Response DTO |
| `src/main/java/io/aether/api/mqtt/AnomalyAlertRelay.java` | Create | @EventListener → AlertPublisher |
| `src/test/java/io/aether/api/handler/InsightHandlerTest.java` | Create | WebTestClient test |
| `src/test/java/io/aether/api/mqtt/AnomalyAlertRelayTest.java` | Create | Unit test with mock AlertPublisher |

## Implementation Hints

- **Handler:**
  ```java
  public Mono<ServerResponse> generateInsight(ServerRequest request) {
      return request.bodyToMono(InsightRequestDto.class)
          .flatMap(req -> insightContextBuilder.build(req.location(), req.asOf()))
          .flatMap(ctx -> insightProvider.generate(ctx))
          .map(insight -> new InsightResponseDto(insight.location(), insight.asOf(),
              insight.summary(), insight.provider()))
          .flatMap(dto -> ServerResponse.ok().bodyValue(dto))
          .onErrorResume(LocationNotFoundException.class,
              e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
  }
  ```
- **AnomalyAlertRelay:**
  ```java
  @EventListener
  public void onAnomaly(AnomalyDetectedEvent event) {
      alertPublisher.publish(event.anomalyEvent());
  }
  ```
- **AlertPublisher.publish topic:**  `"alerts/" + anomaly.location() + "/" + anomaly.metric().toLowerCase()`

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
