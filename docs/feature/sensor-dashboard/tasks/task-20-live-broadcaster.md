# Task 20: Live Broadcaster

**Status:** pending
**HLD Reference:** §Technical Implementation — API Layer (LiveBroadcaster), §Sequence Diagram Part 3

## Description

Implement `LiveBroadcaster` — the bridge between domain application events and the SSE `Flux` streams. It maintains two `Sinks.Many` (one for readings, one for anomalies), listens to `ReadingValidatedEvent` and `AnomalyDetectedEvent`, and exposes `Flux<ServerSentEvent<T>>` that the SSE endpoint handlers subscribe to. This is the only component in `api` that listens to domain events.

## Acceptance Criteria

- [ ] `Sinks.Many<SensorReadingDto>` and `Sinks.Many<AnomalyEventDto>` beans wired in `api` module
- [ ] `@EventListener(ReadingValidatedEvent.class)` → map `SensorReading` → `SensorReadingDto` → `tryEmitNext` to reading sink
- [ ] `@EventListener(AnomalyDetectedEvent.class)` → map `AnomalyEvent` → `AnomalyEventDto` → `tryEmitNext` to anomaly sink
- [ ] `readingStream(String location)` returns `Flux<ServerSentEvent<SensorReadingDto>>` filtered by location
- [ ] `alertStream(String location)` returns `Flux<ServerSentEvent<AnomalyEventDto>>` filtered by location
- [ ] `LiveBroadcasterTest`: publish `ReadingValidatedEvent`; subscribe to `readingStream("surabaya")`; assert event received within 200 ms

## Dependencies

- **Depends on:** Task 04 (domain events, SensorReading, AnomalyEvent)
- **Blocks:** Task 22 (SSE endpoints use the Flux from this component)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/api/sse/LiveBroadcaster.java` | Create | Event listener + Sinks + Flux exposer |
| `src/main/java/io/aether/api/dto/SensorReadingDto.java` | Create | API response DTO for readings |
| `src/main/java/io/aether/api/dto/AnomalyEventDto.java` | Create | API response DTO for alerts |
| `src/main/java/io/aether/api/dto/DtoMapper.java` | Create | SensorReading → SensorReadingDto, etc. |
| `src/test/java/io/aether/api/sse/LiveBroadcasterTest.java` | Create | Unit test using StepVerifier |

## Implementation Hints

- **Sinks + Flux:**
  ```java
  private final Sinks.Many<SensorReadingDto> readingSink =
      Sinks.many().multicast().onBackpressureBuffer(512);

  public Flux<ServerSentEvent<SensorReadingDto>> readingStream(String location) {
      return readingSink.asFlux()
          .filter(dto -> dto.location().equals(location))
          .map(dto -> ServerSentEvent.<SensorReadingDto>builder()
              .event("reading")
              .data(dto)
              .build());
  }
  ```
- **StepVerifier test:**
  ```java
  var flux = broadcaster.readingStream("surabaya");
  var verifier = StepVerifier.create(flux)
      .expectNextMatches(sse -> sse.data().location().equals("surabaya"))
      .thenCancel()
      .verifyLater();
  applicationEventPublisher.publishEvent(new ReadingValidatedEvent(this, reading));
  verifier.verify(Duration.ofSeconds(1));
  ```
- **Key consideration:** `Sinks.many().multicast()` delivers to all current subscribers. A subscriber that connects after the event fires will miss it — this is expected (SSE is not a durable bus; the FE calls `/readings/latest` on reconnect per HLD §SSE reconnect strategy).

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
