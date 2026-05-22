# Task 31: Ingestion Integration Test

**Status:** pending
**HLD Reference:** §Testing Strategy — Ingestion gateway integration, §Milestones M1 acceptance

## Description

Write an end-to-end integration test for the ingestion flow: WireMock serves fixture Open-Meteo responses; a Testcontainers Mosquitto broker receives the published messages; test asserts correct topic, payload structure, and quality flags on a simulated missing/out-of-range value.

## Acceptance Criteria

- [ ] WireMock server stubs both Open-Meteo hosts with fixture JSON responses (3 locations × 24 hourly slots)
- [ ] Testcontainers Mosquitto broker starts; test subscribes to `sensors/#`
- [ ] `OpenMeteoScheduler.poll()` triggered manually (not by cron) in test
- [ ] Assert: messages received on `sensors/surabaya/pm2_5`, `sensors/jakarta/temperature`, etc.
- [ ] Assert: a fixture null value in the AQ response produces a message with `quality.status = SUSPECT` and `quality.flags` containing `MISSING_VALUE`
- [ ] Assert: an out-of-range value produces `OUT_OF_RANGE` flag
- [ ] Assert: `PollCycleCompletedEvent` fired once per location (3 times total)
- [ ] Test completes in < 10 seconds

## Dependencies

- **Depends on:** Task 09 (scheduler + full ingestion flow assembled)
- **Blocks:** Task 34 (CI must pass)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `build.gradle` | Modify | Add `testImplementation 'org.wiremock:wiremock-standalone'`, `'org.testcontainers:mosquitto'` |
| `src/test/java/io/aether/ingestion/IngestionIntegrationTest.java` | Create | Full ingestion flow test |
| `src/test/resources/fixtures/open-meteo-weather-response.json` | Create | Fixture weather response |
| `src/test/resources/fixtures/open-meteo-aq-response.json` | Create | Fixture AQ response (with null and out-of-range values) |

## Implementation Hints

- **Testcontainers Mosquitto (if no official image):** use `GenericContainer("eclipse-mosquitto:2.0.20")` with exposed port 1883 and a minimal mosquitto.conf mounted.
- **Subscribe in test before triggering poll:**
  ```java
  MqttClient testClient = // Paho v5 test client
  testClient.subscribe("sensors/#", 1, (topic, message) -> received.add(topic + "|" + new String(message.getPayload())));
  scheduler.poll(); // trigger manually
  await().atMost(5, SECONDS).until(() -> received.size() >= expectedCount);
  ```
- **Key consideration:** The scheduler by default polls all configured locations. In the test, configure a single test location (`test-location`) pointing to the WireMock server URL to keep the test deterministic and fast.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
