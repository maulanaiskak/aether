# Task 22: SSE Stream Endpoints

**Status:** pending
**HLD Reference:** §API Design — Server-Sent Events, §Functional Requirements FR-14

## Description

Add SSE (Server-Sent Events) endpoints to the WebFlux router. These endpoints return `Flux<ServerSentEvent<T>>` backed by `LiveBroadcaster`. The browser connects once and receives pushed events indefinitely. Include proper `Cache-Control: no-cache` and `Connection: keep-alive` headers required by the SSE protocol.

## Acceptance Criteria

- [ ] `GET /api/v1/stream/readings/{location}` — `text/event-stream`; emits `ServerSentEvent<SensorReadingDto>` for each new reading at that location
- [ ] `GET /api/v1/stream/alerts/{location}` — `text/event-stream`; emits `ServerSentEvent<AnomalyEventDto>` for each new anomaly at that location
- [ ] Response headers: `Content-Type: text/event-stream`, `Cache-Control: no-cache`, `X-Accel-Buffering: no` (for nginx proxy compatibility)
- [ ] Location path variable validated — 400 if location not in configured list
- [ ] SSE events have `event:` field set (`"reading"` or `"alert"`) so the FE can use `EventSource.addEventListener()`
- [ ] `SseEndpointTest` (WebTestClient): subscribe to SSE endpoint; publish a `ReadingValidatedEvent`; assert SSE event received within 2 s (use `StepVerifier`)

## Dependencies

- **Depends on:** Task 20 (LiveBroadcaster), Task 21 (router config pattern)
- **Blocks:** Task 25 (FE connects to these), Task 27 (FE SSE integration), Task 33 (integration test)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/api/router/StreamRouter.java` | Create | RouterFunction for SSE endpoints |
| `src/main/java/io/aether/api/handler/StreamHandler.java` | Create | Handler returning Flux<SSE> |
| `src/test/java/io/aether/api/sse/SseEndpointTest.java` | Create | WebTestClient + StepVerifier test |

## Implementation Hints

- **SSE handler:**
  ```java
  public Mono<ServerResponse> streamReadings(ServerRequest request) {
      var location = request.pathVariable("location");
      var flux = broadcaster.readingStream(location);
      return ServerResponse.ok()
          .header("Cache-Control", "no-cache")
          .header("X-Accel-Buffering", "no")
          .contentType(MediaType.TEXT_EVENT_STREAM)
          .body(flux, new ParameterizedTypeReference<ServerSentEvent<SensorReadingDto>>() {});
  }
  ```
- **SSE test with WebTestClient:**
  ```java
  var result = webTestClient.get()
      .uri("/api/v1/stream/readings/surabaya")
      .accept(MediaType.TEXT_EVENT_STREAM)
      .exchange()
      .expectStatus().isOk()
      .returnResult(new ParameterizedTypeReference<ServerSentEvent<SensorReadingDto>>() {});

  StepVerifier.create(result.getResponseBody())
      .then(() -> eventPublisher.publishEvent(new ReadingValidatedEvent(this, testReading)))
      .expectNextMatches(sse -> "surabaya".equals(sse.data().location()))
      .thenCancel()
      .verify(Duration.ofSeconds(2));
  ```
- **Key consideration:** Spring WebFlux correctly handles `text/event-stream` — it keeps the HTTP connection open and flushes each `ServerSentEvent` as it arrives. No custom streaming infrastructure needed.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
