# Task 01: Project Skeleton

**Status:** pending
**HLD Reference:** §Prerequisites, §Technical Implementation — Stack Summary, §Deployment Plan — Flyway Dual-Datasource Wiring

## Description

Set up the complete project skeleton before any feature code is written. This includes renaming the base package, converting the build file, wiring the Flyway dual-datasource configuration, and creating the module directory structure. All subsequent tasks depend on this foundation being correct.

## Acceptance Criteria

- [ ] Base package renamed from `io.trade.order.aerator` to `io.aether` in all source files, `build.gradle` `group`, and `settings.gradle`
- [ ] `build.gradle` contains exactly the dependencies listed in HLD §Prerequisites: `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql`, `flyway-core`, `postgresql` (JDBC), `spring-integration-mqtt`, `org.eclipse.paho.mqttv5.client:1.2.5`, `resilience4j-reactor`, `resilience4j-circuitbreaker`, `spring-boot-starter-actuator`, `micrometer-registry-prometheus`
- [ ] `spring-boot-starter-webmvc` is absent from all dependency configurations
- [ ] Module package directories exist: `io.aether.domain`, `io.aether.ingestion`, `io.aether.processing`, `io.aether.anomaly`, `io.aether.forecast`, `io.aether.insight`, `io.aether.api`
- [ ] `FlywayConfig` bean wired with `@FlywayDataSource` — Flyway uses JDBC `DataSource`, R2DBC auto-config is unaffected
- [ ] `application.yml` created with `aether.*` config stubs (locations, mqtt, ingestion, anomaly, ml-service, insight)
- [ ] `AeratorApplication.java` moved/renamed to `io.aether.AetherApplication` with `@SpringBootApplication`
- [ ] `./gradlew build` succeeds (no compile errors)
- [ ] `./gradlew test` runs (zero tests, no failures)

## Dependencies

- **Depends on:** —
- **Blocks:** All other tasks

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `build.gradle` | Modify | Update group, replace deps (already partially done) |
| `settings.gradle` | Modify | Rename `rootProject.name` to `aether` |
| `src/main/java/io/aether/AetherApplication.java` | Create | Renamed main class |
| `src/main/java/io/trade/order/aerator/AeratorApplication.java` | Delete | Old main class |
| `src/main/resources/application.yml` | Create | Full config stubs |
| `src/main/resources/application.properties` | Delete | Replace with YAML |
| `src/main/java/io/aether/config/FlywayConfig.java` | Create | `@FlywayDataSource` JDBC bean |
| `src/main/java/io/aether/{domain,ingestion,processing,anomaly,forecast,insight,api}/package-info.java` | Create | Module boundary markers |
| `src/test/java/io/aether/AetherApplicationTests.java` | Create | Renamed test, context loads |

## Implementation Hints

- **FlywayConfig pattern:**
  ```java
  @Configuration
  class FlywayConfig {
      @Bean
      @FlywayDataSource
      DataSource flywayDataSource(
              @Value("${spring.flyway.url}") String url,
              @Value("${spring.flyway.user:aether}") String user,
              @Value("${spring.flyway.password:aether}") String pw) {
          return DataSourceBuilder.create().url(url).username(user).password(pw).build();
      }
  }
  ```
- **application.yml minimum stubs:**
  ```yaml
  spring:
    application.name: aether
    r2dbc:
      url: r2dbc:postgresql://localhost:5432/aether
      username: aether
      password: aether
    flyway:
      url: jdbc:postgresql://localhost:5432/aether
      user: aether
      password: aether
  aether:
    locations:
      - { name: surabaya, lat: -7.2575, lon: 112.7521 }
      - { name: jakarta,  lat: -6.2088, lon: 106.8456 }
      - { name: bandung,  lat: -6.9175, lon: 107.6191 }
    mqtt:
      broker-url: tcp://localhost:1883
      client-id: aerator-01
      qos: 1
    ingestion:
      poll-cron: "0 0 * * * *"
      retry-max-attempts: 3
      retry-backoff-ms: 2000
    anomaly:
      window-size: 48
      zscore-threshold: 3.0
      iqr-multiplier: 1.5
    ml-service:
      base-url: http://ml-service:8000
      forecast-horizon-hours: 24
    insight:
      provider: rule-based
  ```
- **Key consideration:** Spring Boot 4 with R2DBC active: without `@FlywayDataSource`, Flyway silently skips because no `javax.sql.DataSource` bean is auto-created. The `@FlywayDataSource` annotation tells Flyway to use this specific bean.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
