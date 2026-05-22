# Task 09: Ingestion Scheduler

**Status:** pending
**HLD Reference:** §Technical Implementation — Ingestion Gateway (OpenMeteoScheduler), §Sequence Diagram Part 1

## Description

Implement `OpenMeteoScheduler` — the cron-driven orchestrator that polls Open-Meteo for all configured locations, maps responses to `SensorReading` records, publishes each to MQTT, then emits a `PollCycleCompletedEvent` per location. The scheduler runs on a virtual-thread executor so `.subscribe()` blocks the virtual thread without touching the Netty event loop.

## Acceptance Criteria

- [ ] `@Scheduled(cron = "${aether.ingestion.poll-cron}")` fires for all configured locations
- [ ] For each location: calls `OpenMeteoWeatherClient` + `OpenMeteoAirQualityClient` in parallel (`Mono.zip`)
- [ ] Maps responses via `SensorReadingMapper` → `List<SensorReading>`
- [ ] Publishes each `SensorReading` to MQTT via `MqttPublisher`
- [ ] After all readings for a location are published, fires `PollCycleCompletedEvent(location)` via `ApplicationEventPublisher`
- [ ] API downtime (client returns `Mono.empty()`) → logs a warning, skips publish, still fires `PollCycleCompletedEvent`
- [ ] Uses `@EnableScheduling` + virtual thread task executor (Spring Boot 4 auto-configures this with `spring.threads.virtual.enabled=true`)
- [ ] `OpenMeteoSchedulerTest`: mock clients return fixture DTOs; verify correct number of MQTT publishes and one `PollCycleCompletedEvent` per location

## Dependencies

- **Depends on:** Task 06 (clients), Task 07 (mapper), Task 08 (publisher), Task 04 (events)
- **Blocks:** Task 31 (integration test of ingestion flow)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/ingestion/OpenMeteoScheduler.java` | Create | Cron-driven poll orchestrator |
| `src/main/java/io/aether/ingestion/config/SchedulerConfig.java` | Create | `@EnableScheduling` + virtual thread executor |
| `src/main/resources/application.yml` | Modify | Add `spring.threads.virtual.enabled=true` |
| `src/test/java/io/aether/ingestion/OpenMeteoSchedulerTest.java` | Create | Unit test with mocked clients |

## Implementation Hints

- **Virtual thread + reactive mix:**
  ```java
  @Scheduled(cron = "${aether.ingestion.poll-cron}")
  public void poll() {
      for (Location location : locations) {
          // Mono.zip runs both HTTP calls in parallel, then maps/publishes
          Mono.zip(weatherClient.fetch(location), aqClient.fetch(location))
              .flatMapMany(tuple -> Flux.fromIterable(
                  mapper.mapWeather(tuple.getT1(), location)))
              .concatWith(
                  Mono.zip(Mono.just(tuple.getT2()), Mono.just(location))
                      .flatMapMany(t -> Flux.fromIterable(mapper.mapAirQuality(...))))
              .doOnNext(mqttPublisher::publish)
              .doOnComplete(() -> eventPublisher.publishEvent(new PollCycleCompletedEvent(this, location)))
              .blockLast(); // safe on virtual thread
      }
  }
  ```
  (Simplified — actual impl should handle the zip more cleanly)
- **Cleaner pattern:**
  ```java
  Mono.zip(weatherClient.fetch(location).defaultIfEmpty(EMPTY_WEATHER_DTO),
           aqClient.fetch(location).defaultIfEmpty(EMPTY_AQ_DTO))
      .flatMapMany(t -> Flux.fromIterable(mapper.map(t.getT1(), t.getT2(), location)))
      .doOnNext(mqttPublisher::publish)
      .then(Mono.fromRunnable(() -> eventPublisher.publishEvent(new PollCycleCompletedEvent(this, location))))
      .block();
  ```
- **Key consideration:** `block()` is safe here because `@Scheduled` tasks run on the virtual thread pool, not the Netty event loop. Never call `block()` from a method invoked on a Netty thread — it will deadlock.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
