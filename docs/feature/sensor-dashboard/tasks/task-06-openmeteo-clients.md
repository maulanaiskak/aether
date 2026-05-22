# Task 06: Open-Meteo HTTP Clients

**Status:** pending
**HLD Reference:** §Technical Implementation — Ingestion Gateway, §Data Sources

## Description

Implement two reactive HTTP clients that call Open-Meteo's two separate API hosts. Both clients wrap `WebClient` with Resilience4j `CircuitBreaker` + `Retry` (exponential backoff). Each returns a raw JSON DTO that gets mapped in task-07. Neither client has any knowledge of MQTT or the domain `SensorReading` type — they are pure HTTP adapters.

## Acceptance Criteria

- [ ] `OpenMeteoWeatherClient` calls `https://api.open-meteo.com/v1/forecast` with hourly params: `temperature_2m`, `relative_humidity_2m`, `wind_speed_10m`, `surface_pressure`
- [ ] `OpenMeteoAirQualityClient` calls `https://air-quality-api.open-meteo.com/v1/air-quality` with hourly params: `pm2_5`, `pm10`, `ozone`, `nitrogen_dioxide`, `sulphur_dioxide`, `carbon_monoxide`, `us_aqi`, `european_aqi`
- [ ] Both clients accept `Location` and return `Mono<OpenMeteoResponseDto>` (or equivalent raw DTO)
- [ ] Resilience4j `CircuitBreaker` wraps both calls: opens after 3 consecutive failures, half-open after 10 s
- [ ] Resilience4j `Retry` wraps both calls: max 3 attempts, exponential backoff starting at 2 s
- [ ] API downtime (circuit open): method returns `Mono.empty()` (graceful skip, not exception)
- [ ] `OpenMeteoWeatherClientTest` with WireMock: verifies correct URL params, response mapped to DTO, retry fires on 503
- [ ] `OpenMeteoAirQualityClientTest` with WireMock: same pattern

## Dependencies

- **Depends on:** Task 04 (Location domain type), Task 01 (WebClient auto-config via webflux starter)
- **Blocks:** Task 07 (mapper needs DTO types), Task 09 (scheduler calls clients)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/ingestion/client/OpenMeteoWeatherClient.java` | Create | Weather HTTP client |
| `src/main/java/io/aether/ingestion/client/OpenMeteoAirQualityClient.java` | Create | Air quality HTTP client |
| `src/main/java/io/aether/ingestion/dto/OpenMeteoWeatherResponseDto.java` | Create | Jackson DTO for weather response |
| `src/main/java/io/aether/ingestion/dto/OpenMeteoAirQualityResponseDto.java` | Create | Jackson DTO for AQ response |
| `src/main/java/io/aether/ingestion/config/ResilienceConfig.java` | Create | CircuitBreaker + Retry beans |
| `src/main/java/io/aether/ingestion/config/WebClientConfig.java` | Create | WebClient beans with base URLs |
| `src/test/java/io/aether/ingestion/client/OpenMeteoWeatherClientTest.java` | Create | WireMock test |
| `src/test/java/io/aether/ingestion/client/OpenMeteoAirQualityClientTest.java` | Create | WireMock test |

## Implementation Hints

- **Resilience4j reactive pattern:**
  ```java
  return CircuitBreakerOperator.of(circuitBreaker)
      .apply(RetryOperator.of(retry)
          .apply(webClient.get().uri(...).retrieve().bodyToMono(ResponseDto.class)))
      .onErrorResume(CallNotPermittedException.class, e -> Mono.empty())
      .onErrorResume(e -> { log.warn("Open-Meteo call failed: {}", e.getMessage()); return Mono.empty(); });
  ```
- **Open-Meteo URL format (weather):**
  `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,surface_pressure&timezone=UTC&forecast_days=1`
- **Open-Meteo URL format (AQ):**
  `https://air-quality-api.open-meteo.com/v1/air-quality?latitude={lat}&longitude={lon}&hourly=pm2_5,pm10,ozone,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide,us_aqi,european_aqi&timezone=UTC&forecast_days=1`
- **Key consideration:** The two APIs use different base URLs (`api.open-meteo.com` vs `air-quality-api.open-meteo.com`). Create two separate `WebClient` beans with different base URLs — do not share one bean.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
