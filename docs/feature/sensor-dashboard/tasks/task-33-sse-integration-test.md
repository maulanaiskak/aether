# Task 33: SSE Integration Test

**Status:** pending
**HLD Reference:** §Testing Strategy — SSE live push, §NFR-01 (≤ 5 s latency)

## Description

End-to-end integration test for the SSE push path: publish a `ReadingValidatedEvent` programmatically, connect a `WebTestClient` to the SSE endpoint, and assert the event arrives within 2 seconds. This test validates the full `ApplicationEvent → LiveBroadcaster → Sinks.Many → SSE` chain without going through MQTT.

## Acceptance Criteria

- [ ] `WebTestClient` subscribes to `GET /api/v1/stream/readings/surabaya` with `Accept: text/event-stream`
- [ ] Test publishes `ReadingValidatedEvent` for location `surabaya` via `ApplicationEventPublisher`
- [ ] `StepVerifier` asserts SSE event received within 2 seconds with correct `data.location == "surabaya"`
- [ ] Second test: alert path — publish `AnomalyDetectedEvent`; assert SSE event on `/api/v1/stream/alerts/surabaya`
- [ ] Test runs with `@SpringBootTest(webEnvironment = RANDOM_PORT)` — no Testcontainers needed (in-memory R2DBC or H2 for context load)

## Dependencies

- **Depends on:** Task 22 (SSE endpoints), Task 20 (LiveBroadcaster)
- **Blocks:** Task 34 (CI)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/test/java/io/aether/api/SseIntegrationTest.java` | Create | End-to-end SSE push test |

## Implementation Hints

- **Full test:**
  ```java
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  class SseIntegrationTest {
      @Autowired WebTestClient webTestClient;
      @Autowired ApplicationEventPublisher eventPublisher;

      @Test
      void reading_event_reaches_sse_stream() {
          var reading = testReading("surabaya", Metric.PM2_5, 38.4);
          var result = webTestClient.get()
              .uri("/api/v1/stream/readings/surabaya")
              .accept(MediaType.TEXT_EVENT_STREAM)
              .exchange()
              .expectStatus().isOk()
              .returnResult(new ParameterizedTypeReference<ServerSentEvent<SensorReadingDto>>() {});

          StepVerifier.create(result.getResponseBody())
              .then(() -> eventPublisher.publishEvent(new ReadingValidatedEvent(this, reading)))
              .expectNextMatches(sse -> "surabaya".equals(sse.data().location()))
              .thenCancel()
              .verify(Duration.ofSeconds(2));
      }
  }
  ```
- **Key consideration:** `@SpringBootTest` will attempt to connect to Mosquitto and Postgres at startup. Use `@MockBean` for MQTT adapters and an embedded H2 R2DBC datasource in test profile, or use Testcontainers with `@DynamicPropertySource` to inject real connection URLs. The latter is more realistic but slower.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
