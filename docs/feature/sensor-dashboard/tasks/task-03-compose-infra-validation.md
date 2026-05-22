# Task 03: Docker Compose Infrastructure Validation

**Status:** pending
**HLD Reference:** §Deployment Plan — Docker Compose Services, §Milestones M0

## Description

Create the full `docker-compose.yml` and supporting infrastructure config files (Mosquitto config). Validate that `docker compose up` brings all infrastructure services healthy, Flyway migrations run successfully, and a heartbeat MQTT round-trip proves broker connectivity. This is the M0 acceptance gate — the project is only unblocked for feature work once this passes.

## Acceptance Criteria

- [ ] `docker-compose.yml` defines: `mosquitto`, `postgres`, `aerator`, `ml-service` (stub), `frontend` (stub) with correct health checks and `condition: service_healthy` dependencies
- [ ] `infra/mosquitto/mosquitto.conf` created (anonymous mode, persistence off for dev)
- [ ] `docker compose up` → all services healthy within 3 minutes on a fresh machine
- [ ] `./actuator/health` returns `{"status":"UP"}` from the aerator container
- [ ] Flyway migrations applied: `flyway_schema_history` table shows V1–V6 as SUCCESS
- [ ] Aerator publishes a heartbeat message to `aether/system/heartbeat` every 30 s (observable via `mosquitto_sub`)
- [ ] `.env.example` documents `POSTGRES_PASSWORD`

## Dependencies

- **Depends on:** Task 01 (project skeleton), Task 02 (migrations)
- **Blocks:** Task 09 (scheduler needs running broker), Task 31 (integration test)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `docker-compose.yml` | Create | Full compose stack definition |
| `infra/mosquitto/mosquitto.conf` | Create | Mosquitto broker config (anonymous, port 1883) |
| `.env.example` | Create | Documents required env vars |
| `src/main/java/io/aether/api/MqttHeartbeatPublisher.java` | Create | 30-second heartbeat to `aether/system/heartbeat` |
| `Dockerfile` | Create | Multi-stage Gradle build → Spring Boot JVM image |

## Implementation Hints

- **docker-compose.yml key points:**
  ```yaml
  services:
    mosquitto:
      image: eclipse-mosquitto:2.0.20
      volumes: ["./infra/mosquitto/mosquitto.conf:/mosquitto/config/mosquitto.conf"]
      healthcheck:
        test: ["CMD", "mosquitto_pub", "-t", "health", "-m", "ping", "-q", "0"]
        interval: 5s
        retries: 10
    postgres:
      image: timescale/timescaledb:2.17.2-pg16
      healthcheck:
        test: ["CMD", "pg_isready", "-U", "aether"]
        interval: 5s
        retries: 10
    aerator:
      depends_on:
        mosquitto: { condition: service_healthy }
        postgres:  { condition: service_healthy }
  ```
- **mosquitto.conf minimum:**
  ```
  listener 1883
  allow_anonymous true
  persistence false
  ```
- **Dockerfile multi-stage:**
  ```dockerfile
  FROM eclipse-temurin:21-jdk AS build
  WORKDIR /app
  COPY gradlew build.gradle settings.gradle ./
  COPY gradle gradle
  RUN ./gradlew dependencies --no-daemon
  COPY src src
  RUN ./gradlew bootJar --no-daemon

  FROM eclipse-temurin:21-jre
  COPY --from=build /app/build/libs/*.jar app.jar
  ENTRYPOINT ["java","-jar","/app.jar"]
  ```
- **Key consideration:** The `aerator` healthcheck uses `curl`. Ensure `curl` is present in the JRE base image or use `wget` instead. Eclipse Temurin JRE images include neither by default — add `RUN apt-get install -y curl` in the final stage or use the actuator HTTP check via a Java one-liner.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
