# Aether — Real-Time Environmental Sensor Dashboard

A portfolio project demonstrating end-to-end reactive backend engineering: HTTP-pull ingestion → MQTT streaming → time-series persistence → ML forecasting → REST + SSE API → live React dashboard.

---

## Honesty Disclosures

1. **Data is CAMS modelled forecasts, not physical sensor readings.** Open-Meteo serves numerical weather model output, not measurements from physical hardware.
2. **"Real-time" describes pipeline reactivity (< 5 s MQTT → browser), not data freshness (hourly upstream).** The pipeline streams data fast; the upstream source updates hourly.
3. **The PM2.5 forecast is a cheap surrogate/now-cast over the canonical pipeline, not an attempt to out-predict CAMS.** The LightGBM model acts as a smoothing / short-horizon interpolation layer.

---

## Architecture

```
Open-Meteo APIs ──► OpenMeteoScheduler ──► MQTT (Mosquitto)
                                                │
                                     MqttReadingConsumer
                                                │
                                  ReadingPersistenceHandler
                                  (validate → upsert → flags)
                                                │
                             ┌──────────────────┼──────────────────┐
                             │                  │                  │
                      SmoothingService   GapFillService   AnomalyOrchestrator
                                                             │
                                                      AlertPublisher (MQTT)
                                                             │
                                              ForecastOrchestrator ──► ml-service (Python)
                                                             │
                                                     InsightEngine
                                                             │
                                              REST API + SSE (WebFlux)
                                                             │
                                                   React Dashboard
```

**Stack:**
- Backend: Spring Boot 4 (WebFlux + R2DBC, virtual threads)
- Database: TimescaleDB 2.x on PostgreSQL 16
- Broker: Eclipse Mosquitto 2.x (MQTT v5)
- ML Sidecar: Python 3.12 + FastAPI + LightGBM
- Frontend: React 19 + Vite 6 + TanStack Query + Recharts

---

## Quickstart

```bash
git clone <repo-url>
cd aether
cp .env.example .env
docker compose up
```

Dashboard opens at **http://localhost** once all services are healthy (≈ 2–3 min on first run while the ML model trains).

---

## MQTT Topic Map

| Topic | Direction | Description |
|-------|-----------|-------------|
| `sensors/{location}/{metric}` | ingestion → broker | Raw sensor readings |
| `alerts/{location}/{metric}` | api → broker | Anomaly events |
| `aether/system/heartbeat` | backend → broker | 30 s health pulse |

Locations: `surabaya`, `jakarta`, `bandung`

Metrics: `pm2_5`, `pm10`, `temperature`, `humidity`, `wind_speed`, `pressure`, `o3`, `no2`, `so2`, `co`, `us_aqi`, `eu_aqi`

---

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/readings` | Paginated historical readings |
| GET | `/api/v1/readings/latest` | Latest reading per metric/location |
| GET | `/api/v1/forecast` | 24-h PM2.5 forecast from ML sidecar |
| GET | `/api/v1/anomalies` | Recent anomaly events |
| POST | `/api/v1/insight` | Generate rule-based insight |
| GET | `/api/v1/stream/readings/{location}` | SSE live reading stream |
| GET | `/api/v1/stream/alerts/{location}` | SSE anomaly alert stream |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |

---

## Running Tests

```bash
./gradlew test
```

Tests include: domain unit tests, validation pipeline, anomaly detectors, ArchUnit module boundary checks, SSE integration test, and processing validation tests.

---

## Attribution

- Weather data: [Open-Meteo](https://open-meteo.com/) — licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- Air quality model: [Copernicus Atmosphere Monitoring Service (CAMS)](https://atmosphere.copernicus.eu/)

---

## Documentation

Full high-level design: [`docs/feature/sensor-dashboard/HLD-sensor-dashboard.md`](docs/feature/sensor-dashboard/HLD-sensor-dashboard.md)
