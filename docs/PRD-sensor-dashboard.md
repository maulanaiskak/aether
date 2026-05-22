# PRD — Real-Time Environmental Sensor Dashboard

> **Project codename:** `aether` (atau bebas)
> **Type:** Personal / portfolio showcase (open-source, public GitHub)
> **Author:** Maulana
> **Status:** v1 (final — local-first)
> **Last updated:** 2026-05-22

---

## 1. Summary

A real-time environmental monitoring platform that ingests **weather + air-quality data** from public open APIs, streams it through an **MQTT-based pipeline**, applies **signal processing + ML forecasting**, generates **deterministic rule-based insights**, and presents everything on a **React dashboard**.

> **v1 scope note (2026-05-22):** LLM-based natural-language insight is **deferred to v2**. v1 ships a deterministic, rule-based insight engine behind a stable `InsightProvider` interface, so the LLM implementation can be swapped in later without touching the API or FE contract.

The project is deliberately scoped as a *showcase*: it is meant to demonstrate end-to-end backend engineering competence (ingestion → streaming → processing → ML → API → realtime FE), not to be a production environmental product.

> **Honesty note on "real-time" (read before §4):** the *pipeline* is streaming/real-time (MQTT → processing → WebSocket push to FE in seconds). The *upstream data cadence is hourly* — Open-Meteo updates its models on an hourly-ish schedule, and the air-quality endpoint returns hourly values. So "real-time" describes the **transport and dashboard reactivity**, not the freshness of the underlying physical measurement. This distinction is stated plainly in the README; conflating the two is the most common way this kind of project loses credibility with a sharp reviewer.

---

## 2. Goals & Non-Goals

### 2.1 Goals
- **G1** — Ingest weather + air-quality data from public APIs on a schedule (pull) and normalize into a canonical sensor model.
- **G2** — Demonstrate an **IoT-native streaming pipeline** using MQTT as the transport, even though the source is HTTP-pull (see §5 design decision).
- **G3** — Apply **signal processing** (smoothing, gap-filling, anomaly detection) on incoming time-series.
- **G4** — Provide **short-horizon ML forecasting** of a target variable (PM2.5 / AQI).
- **G5** — Provide a **rule-based insight** capability that explains current conditions / anomalies / forecast in plain language, behind a swappable `InsightProvider` interface (LLM impl deferred to v2).
- **G6** — Present live + historical data on a **React dashboard** with realtime updates.
- **G7** — Be **runnable by anyone** via `docker compose up` with minimal/zero secret setup. **This is also the deployment target — v1 is local-first, no hosted environment (see §16).**

### 2.2 Non-Goals
- ❌ Not a production-grade or commercially accurate environmental service.
- ❌ No real physical IoT hardware (data is simulated-via-public-API; documented honestly).
- ❌ No user accounts / auth / multi-tenancy (out of scope for v1).
- ❌ No mobile app.
- ❌ Not optimized for horizontal scale (single-node compose is the target).

---

## 3. Target Audience (of the showcase)

- Hiring managers / tech leads reviewing the GitHub repo.
- Graduate program reviewers (relevance to IoT data-quality research — Track 2).
- Yourself, as a learning vehicle for MQTT + time-series processing + ML serving.

---

## 4. Success Metrics (portfolio-oriented)

| Metric | Target |
|---|---|
| `docker compose up` → working dashboard | < 3 min, zero manual config |
| README clarity (architecture diagram + GIF demo) | present |
| Test coverage on ingestion + processing core | meaningful unit + 1 integration test |
| MQTT publish → dashboard render latency | < 5 s (transport reactivity, *not* data freshness — see honesty note) |
| Idempotent ingestion | re-polling never creates duplicate rows (unique `sensor_id + observed_at`) |
| Forecast pipeline | reproducible, documented model + metrics |
| Insight engine | deterministic, no external dependency, behind swappable interface |

---

## 5. Key Architectural Decision: HTTP-Pull source over an MQTT pipeline

**Context.** Public weather/AQ APIs (Open-Meteo, etc.) are **HTTP GET / pull** sources. They do not push over MQTT. A naive design would just poll → store → display, and MQTT would add nothing.

**Decision.** Introduce an explicit **Ingestion Gateway** (a "virtual sensor" / edge simulator) that:
1. Polls public APIs on a schedule.
2. Normalizes each reading into a canonical `SensorReading` envelope.
3. **Publishes** each reading to an MQTT broker on topic-per-sensor (e.g. `sensors/{location}/{metric}`).

The rest of the system consumes from MQTT as if the data came from real field devices.

**Why this is good for a showcase (not a hack):**
- It models the *real* IoT pattern: edge gateways translating heterogeneous sources into a uniform MQTT bus. This is exactly how many real deployments bridge legacy/HTTP sources onto an IoT backbone. *(framed honestly in README — no pretending these are physical sensors)*
- It cleanly separates *acquisition* from *transport* from *processing*.
- It directly maps to your research interest (semantic validation of heterogeneous IoT data before downstream use).

> **[Inference]** Reviewers tend to respond well to an explicit, documented design decision like this — it signals you understood the impedance mismatch instead of bolting MQTT on for buzzword reasons. Not guaranteed, but it's a strong narrative for the README.

---

## 6. System Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          INGESTION GATEWAY (Spring Boot)                   │
│  @Scheduled poll → Open-Meteo (weather + air-quality)                      │
│  normalize → SensorReading envelope → publish MQTT                         │
└───────────────┬──────────────────────────────────────────────────────────┘
                │ publish  topic: sensors/{loc}/{metric}
                ▼
        ┌───────────────┐
        │  MQTT BROKER  │   (Eclipse Mosquitto)
        │  (Mosquitto)  │
        └───────┬───────┘
                │ subscribe
                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                       PROCESSING SERVICE (Spring Boot)                     │
│  MQTT subscriber (Spring Integration MQTT)                                 │
│   ├─ persist raw → Postgres (TimescaleDB extension)                        │
│   ├─ signal processing: smoothing, gap-fill, range/stale validation        │
│   ├─ anomaly detection (z-score / EWMA / IQR)                              │
│   └─ publish processed + alerts → internal topic / WS                      │
└───────────────┬───────────────────────────────────┬──────────────────────┘
                │                                     │
                ▼                                     ▼
   ┌────────────────────────┐          ┌──────────────────────────────┐
   │  ML FORECAST SERVICE   │          │  RULE-BASED INSIGHT          │
   │  PM2.5/AQI short-horizon│          │  (InsightProvider interface; │
   │  (see §9 for options)  │          │   LLM impl deferred to v2)    │
   └───────────┬────────────┘          └──────────────┬───────────────┘
               │                                       │
               └───────────────┬───────────────────────┘
                               ▼
                  ┌──────────────────────────┐
                  │   REST + WebSocket API   │   (Spring Boot)
                  │   /api/readings, /forecast│
                  │   /insight, ws://live     │
                  └────────────┬─────────────┘
                               ▼
                  ┌──────────────────────────┐
                  │     REACT DASHBOARD      │
                  │  live charts, map, AQI,  │
                  │  forecast band, insight  │
                  └──────────────────────────┘
```

> v1 collapses Ingestion + Processing + Forecast-orchestration + Insight + API into **one Spring Boot module** (modular monolith). **The boxes above are *logical modules / packages*, not separate deployables in v1** — they map to packages (`io.aether.ingestion`, `.processing`, …) with enforced inward-only dependencies on a shared `domain` package. Split into real services only if you want to show microservice chops. **Recommendation: modular monolith for v1.** (The one genuinely separate process is the Python ML service — see §9.)

### 6.1 Data contract (the system's spine)

The MQTT payload is the contract every module depends on, so it is governed explicitly:

- **Format:** JSON (human-readable, trivially debuggable with `mosquitto_sub`; revisit Avro/Protobuf only if throughput ever mattered, which it doesn't here).
- **Schema:** the `SensorReading` envelope (§7) carries a `schemaVersion` field. Consumers reject unknown major versions loudly rather than silently mis-parsing.
- **Topic taxonomy:** `sensors/{location}/{metric}` for raw readings; `alerts/{location}/{metric}` for anomalies; `aether/system/#` reserved for health/heartbeat. Documented in the README as a small "topic map."
- **Idempotency:** `(sensor_id, observed_at)` is the natural key. Because hourly data is re-polled, ingestion **upserts** on this key (`ON CONFLICT DO NOTHING`/`DO UPDATE`) — re-fetching the same hour must never duplicate rows. This is enforced by a DB unique constraint (§11), not just app logic.
- **Time:** everything stored and transported in **UTC** (`TIMESTAMPTZ`); the FE localizes for display only.

---

## 7. Data Sources

| Source | Data | Auth | Host + Endpoint |
|---|---|---|---|
| Open-Meteo Forecast | temp, humidity, wind, pressure | none | `https://api.open-meteo.com/v1/forecast` |
| Open-Meteo Air Quality | PM2.5, PM10, O3, NO2, SO2, CO, US/EU AQI | none | `https://air-quality-api.open-meteo.com/v1/air-quality` |

> **Two different hosts** — the air-quality API lives on `air-quality-api.open-meteo.com`, not the forecast host. Easy to get wrong; the ingestion gateway must hold two base URLs.

Both are key-less, CORS-enabled, JSON, **CC BY 4.0** (attribution required — see §17). Poll a configurable list of locations (default: a few Indonesian cities incl. Sidoarjo/Surabaya).

> **Critical data-semantics caveat (affects the whole ML story):** Open-Meteo's air-quality values are themselves the output of the **CAMS forecast models**, not ground-truth sensor observations. They are *modelled forecasts*, hourly, for "today" onward. Implications:
> - This is **not** a real physical sensor feed; the README must say so (ties to the §5 "virtual sensor" framing — be consistent and honest).
> - For the forecasting milestone, predicting Open-Meteo AQ from Open-Meteo weather is **predicting one model's output from another model's output** → high circularity / leakage risk (see §9.1). The defensible framing is "learning a cheap surrogate / now-cast over the canonical pipeline," not "beating CAMS."
> - If you want genuine *observations* (to make forecasting honest), add a real measurement source as a stretch — e.g. OpenAQ / PurpleAir ground stations (§15). **[Inference]** this would meaningfully strengthen the research narrative.

**Canonical envelope (`SensorReading`):**
```jsonc
{
  "sensorId": "open-meteo:surabaya:pm2_5",   // convention: source:location:metric
  "schemaVersion": 1,                          // payload contract version (see §6.1)
  "location": { "name": "surabaya", "lat": -7.2575, "lon": 112.7521 },
  "metric": "pm2_5",
  "unit": "µg/m³",
  "value": 38.4,                               // null allowed; quality.flags carries MISSING_VALUE
  "observedAt": "2026-05-22T03:00:00Z",        // source-attributed timestamp (idempotency key)
  "ingestedAt": "2026-05-22T03:00:12Z",
  "source": "open-meteo",
  "quality": { "status": "OK", "flags": [] }
}
```

---

## 8. Functional Requirements

### 8.1 Ingestion
- **FR-1** Poll configured locations + metrics on a cron/fixed-rate schedule.
- **FR-2** Map provider response → `SensorReading`. Handle missing fields → quality flag, not crash.
- **FR-3** Publish each reading to MQTT `sensors/{loc}/{metric}` (QoS 1).
- **FR-4** Be resilient to API downtime (retry w/ backoff, circuit breaker).

### 8.2 Processing & Signal

> **Conceptual split (don't blur these — it's the core of the data-quality narrative):**
> - **Validation** answers *"is this datum structurally trustworthy?"* — parse success, physical range, staleness, presence. A failure means the value is **defective** → `REJECTED`/`SUSPECT`, do not feed it to models. *(deterministic, per-datum)*
> - **Anomaly detection** answers *"is this a statistically/behaviourally unusual but otherwise valid value?"* — a real, well-formed PM2.5 reading that is 4σ above the local trend. An anomaly is **interesting**, not defective. *(statistical, needs history)*
> A spike can be both (validation rejects an impossible jump; anomaly flags a large-but-plausible one) — the rate-of-change check and the statistical detector use different thresholds and emit different outcomes.

- **FR-5** Subscribe to MQTT, **upsert** raw readings into the Timescale hypertable on `(sensor_id, observed_at)` (idempotent re-polling — see §6.1).
- **FR-6** **Smoothing**: rolling mean / EWMA per metric for display + denoise (stored separately or computed on read; never overwrites raw).
- **FR-7** **Gap-filling**: detect missing hourly intervals, interpolate, flag imputed values `IMPUTED` (never silently fabricate).
- **FR-8** **Validation** (per-datum, deterministic — the data-quality core):
  - parse / type / unit check
  - physical range check (e.g. PM2.5 ∈ [0, 1000] µg/m³)
  - staleness check (`observedAt` too old relative to now)
  - presence check (null value → `MISSING_VALUE` flag, not crash)
  - → produces a `Quality{status, flags}` verdict attached to the reading
- **FR-9** **Anomaly detection** (statistical, needs history): z-score / EWMA-residual / IQR over the recent window; emit `alerts/{loc}/{metric}` events and persist to `anomaly`. Operates only on **validated** (non-rejected) readings.

### 8.3 Forecasting
- **FR-10** Produce a short-horizon forecast (next **6–24 h**, hourly steps) for **PM2.5** (single target — AQI is a derived step-function of PM2.5/PM10/etc. and is ugly to regress directly; derive AQI *from* the PM2.5 forecast for display if needed).
- **FR-11** Store forecast + confidence band; expose via API.
- **FR-12** Track + expose model error metrics (MAE/RMSE) on a held-out test split; show a naive baseline (persistence / seasonal-naive) alongside so the model's value is legible. **Honestly report if the model barely beats the baseline** — see §9.1.

### 8.4 Insight (rule-based, v1)
- **FR-13** Define an `InsightProvider` interface: `Insight generate(InsightContext ctx)` where `ctx` holds recent window, forecast, and active anomalies.
- **FR-14** Ship a `RuleBasedInsightProvider` (v1 default): deterministic templated summaries driven by thresholds + trend slope + anomaly events. Examples: "AQI trending up over next 8h (low wind, rising humidity)"; "PM2.5 spike at 02:00 flagged anomalous (z-score 3.4)".
- **FR-14b** `POST /api/insight` returns the generated insight. No external dependency, no API key.
- **FR-14c** *(v2)* `LlmInsightProvider` swaps in behind the same interface — API + FE contract unchanged.

### 8.5 API & Realtime
- **FR-15** REST: `/api/readings` (query by location/metric/time range), `/api/forecast`, `/api/anomalies`.
- **FR-16** WebSocket (STOMP) channel for live reading + alert push to FE.

### 8.6 Dashboard
- **FR-17** Live multi-metric time-series charts (per location).
- **FR-18** Current AQI gauge + color-coded status.
- **FR-19** Forecast overlay with confidence band.
- **FR-20** Anomaly markers on the timeline.
- **FR-21** "Explain this" button → calls insight endpoint, shows rule-based summary.
- **FR-22** Map view of monitored locations (optional v1.1).

---

## 9. ML Approach — options (pick one, document the choice)

> **[Inference]** All three are defensible; trade-offs below.

| Option | What | Pros | Cons |
|---|---|---|---|
| **A. In-JVM baseline** | EWMA / Holt-Winters / simple AR in Java | zero extra service, fully reproducible, no Python | weakest "ML" story |
| **B. Python sidecar (recommended)** | Train e.g. Prophet / LightGBM / small LSTM in Python, serve via FastAPI; Spring calls it | real ML story, clean separation, common industry pattern | adds a service + language |
| **C. ONNX in JVM** | Train in Python, export ONNX, run inference in Java via onnxruntime | single runtime at serve time, "no Python at runtime" flex | export friction, debugging harder |

**Recommendation:** **Option B** for v1 (clearest ML narrative + most realistic), with a note in README that **C** is the production-leaning path. Use weather features (wind, humidity, temp) as exogenous regressors → PM2.5. That feature-engineering story is itself a talking point.

### 9.1 ML methodology guardrails (read before building M6)

These are the traps that a grad-program reviewer *will* probe. Naming them in the PRD turns a liability into evidence of rigour.

- **Leakage / circularity (the big one):** Open-Meteo PM2.5 is itself a CAMS *forecast*, and the weather features are *forecasts* from the same provider family. Predicting one from the other risks "learning the model that already exists." **Mitigations:** (a) frame the task honestly as a *now-cast / cheap surrogate* over the canonical pipeline, not as out-forecasting CAMS; (b) for any feature used at predict-time, use only values that would be known at inference time — never feed the same-hour AQ truth as a feature for that hour; (c) ideally validate against a *different* source (OpenAQ ground observations) so target ≠ same model family.
- **Temporal split, not random split:** time-series CV must respect time order (train on past, test on future). A random k-fold leaks future into past and inflates metrics. Use a forward-chaining / expanding-window split.
- **Baseline is mandatory:** report persistence ("next hour = this hour") and seasonal-naive ("same hour yesterday") as baselines. A fancy model that doesn't beat seasonal-naive is a finding, not a failure — report it.
- **Stationarity / scaling:** difference or detrend if needed; document it. Don't standardize using stats computed over the full series (that leaks test distribution into train).
- **Honest metrics:** MAE + RMSE on the held-out future window, per horizon step (error grows with horizon — show the curve).

---

## 10. Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Language | **Java 21 (LTS)** | records, pattern matching, virtual threads for pollers; matches your day-job stack |
| Framework | **Spring Boot 3.5.x** | pinned deliberately over 4.0: 3.5 is the mature branch with the most working MQTT references and is OSS-patched into mid-2026; 4.0 (Spring Framework 7 / Spring Integration 7) is newer with fewer field examples. Trivially upgradable later. |
| MQTT | **Eclipse Mosquitto** + **Spring Integration MQTT (Paho v5 client)** | use the v5 adapters, not the ancient mqttv3 snippets that dominate old tutorials |
| Insight | **Java (rule engine)**; Spring AI *(v2)* | deterministic v1; LLM later behind interface |
| DB | **PostgreSQL + TimescaleDB** | hypertables for time-series, continuous aggregates |
| Migrations | **Flyway** | |
| ML serve | **Python + FastAPI** (Option B) | forecasting model |
| FE | **React + Vite + TypeScript** | |
| FE charts | **Recharts** or **visx**; map via **MapLibre** | |
| FE state/data | **TanStack Query** + native WebSocket/STOMP | |
| Build | **Gradle** (`spotlessApply`, googleJavaFormat) | matches your toolchain |
| Container | **Docker Compose** | broker + db + backend + ml + fe |
| CI | **GitHub Actions** | build, test, lint, docker build |
| Observability (nice-to-have) | **Micrometer + Prometheus + Grafana** | shows ops maturity |

---

## 11. Data Model (Postgres / Timescale)

```sql
-- Enable TimescaleDB (Postgres extension) — run as the first Flyway migration.
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- raw + validated readings (time-series hypertable)
CREATE TABLE sensor_reading (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    sensor_id      TEXT             NOT NULL,
    location       TEXT             NOT NULL,
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    metric         TEXT             NOT NULL,
    unit           TEXT             NOT NULL,
    value          DOUBLE PRECISION,                 -- nullable; quality_flags carries MISSING_VALUE
    observed_at    TIMESTAMPTZ      NOT NULL,
    ingested_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    source         TEXT             NOT NULL,
    quality_status TEXT             NOT NULL DEFAULT 'OK',
    quality_flags  TEXT[]           NOT NULL DEFAULT '{}',
    PRIMARY KEY (id, observed_at),                    -- hypertable PK must include the time column
    -- idempotency: re-polling the same hour must not duplicate (see §6.1)
    UNIQUE (sensor_id, observed_at)
);
SELECT create_hypertable('sensor_reading', 'observed_at', if_not_exists => TRUE);
CREATE INDEX idx_reading_loc_metric_time
    ON sensor_reading (location, metric, observed_at DESC);

CREATE TABLE forecast (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location     TEXT             NOT NULL,
    metric       TEXT             NOT NULL,
    horizon_at   TIMESTAMPTZ      NOT NULL,           -- timestamp being predicted FOR
    predicted    DOUBLE PRECISION NOT NULL,
    lower_bound  DOUBLE PRECISION,
    upper_bound  DOUBLE PRECISION,
    model        TEXT             NOT NULL,
    created_at   TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX idx_forecast_loc_metric_horizon
    ON forecast (location, metric, horizon_at DESC);

CREATE TABLE anomaly (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sensor_id    TEXT             NOT NULL,
    location     TEXT             NOT NULL,
    metric       TEXT             NOT NULL,
    observed_at  TIMESTAMPTZ      NOT NULL,
    value        DOUBLE PRECISION,
    method       TEXT             NOT NULL,           -- ZSCORE | EWMA | IQR
    score        DOUBLE PRECISION,
    detected_at  TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX idx_anomaly_loc_metric_time
    ON anomaly (location, metric, observed_at DESC);
```
*Note: the hypertable partitioning column (`observed_at`) must be part of any primary/unique key, hence the composite `PRIMARY KEY (id, observed_at)` plus the `UNIQUE (sensor_id, observed_at)` idempotency constraint.*

---

## 12. Milestones

| # | Milestone | Deliverable | Definition of Done (acceptance) |
|---|---|---|---|
| M0 | Repo + compose skeleton | Mosquitto + Postgres(Timescale) + Spring Boot boots | `docker compose up` → app `/actuator/health` is `UP`; Flyway migrations applied; app proves it can publish+subscribe a heartbeat on MQTT |
| M1 | Ingestion gateway | polls Open-Meteo (both hosts) → publishes MQTT | a configured location produces `SensorReading` messages on `sensors/{loc}/{metric}`; missing fields → quality flag, no crash; API-down → backoff, app stays up |
| M2 | Processing + persistence | MQTT→Timescale, smoothing, validation | readings upserted idempotently (re-poll = no dup rows); each row has a `Quality` verdict; out-of-range/stale values correctly flagged |
| M3 | Anomaly detection | alerts emitted + stored | an injected spike on a validated series produces an `anomaly` row + `alerts/...` message; no anomalies emitted for `REJECTED` readings |
| M4 | REST + WebSocket API | queryable + live push | `/api/readings` returns range-filtered data; a new MQTT reading pushes to a connected WS client in < 5 s |
| M5 | React dashboard (live) | charts + AQI + live updates | dashboard renders live multi-metric chart + AQI gauge for a location and updates without refresh |
| M6 | ML forecasting | Python service + forecast overlay | `/api/forecast` returns a 6–24h PM2.5 forecast with band; metrics page shows MAE/RMSE **vs a naive baseline** on a temporal split (§9.1) |
| M7 | Rule-based insight | `InsightProvider` + `RuleBasedInsightProvider` + FE button | "Explain this" returns a deterministic summary from current window+forecast+anomalies; zero external deps |
| M8 | Polish | README, arch diagram, demo GIF, CI, tests | README has arch diagram + honesty notes + topic map + attribution; CI green (build+test+lint); demo GIF present |
| v2 | LLM insight (Spring AI) | swap `LlmInsightProvider` behind same interface | LLM impl selectable by config; rule-based remains default; API/FE contract unchanged |

> **Recommendation:** Ship M0–M5 first as a self-contained "v1 live dashboard" — already demo-able. M6 (ML forecast) is the main differentiator; M8 is what actually gets you noticed. M7 insight is small once the interface exists.

---

## 13. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Public API rate limits / downtime | backoff + cache last good; low poll frequency (hourly model anyway) |
| "Fake IoT" perception | document the gateway pattern honestly in README (§5) |
| LLM cost / key requirement | n/a in v1 — insight is rule-based & dependency-free; LLM is v2 behind interface |
| Scope creep | M0–M5 is the floor; everything else is additive |
| Forecast barely beats baseline | expected & fine — report it honestly vs naive baseline (§9.1); the data-quality pipeline is the real story, not SOTA accuracy |
| ML leakage / circularity | named and mitigated in §9.1; framed as now-cast surrogate, temporal split, optional real-obs validation |
| Duplicate rows from re-polling | DB-level `UNIQUE (sensor_id, observed_at)` + upsert (§6.1, §11) |

---

## 14. Open Questions

1. Single modular monolith vs split services for v1? *(rec: monolith)*
2. ML serving: Option B vs C? *(rec: B now, mention C)*
3. How many locations to monitor by default? *(rec: 3–5)*
4. ~~Deploy publicly or compose-only?~~ **Resolved: local-first.** v1 runs entirely on `docker compose up`; no hosted deployment. Free-tier hosting in 2026 can't hold this stack always-on, and for a GitHub showcase a clean local run + demo GIF + README satisfies almost all reviewers. Public deploy is a documented v2 path (§17), not a v1 goal.
5. *(v2)* LLM provider for Spring AI — local Ollama (zero-cost demo) vs hosted?

---

## 15. Stretch / v2 ideas
- Add **seismic** (USGS) as a 3rd heterogeneous source → strengthens the "data-quality across heterogeneous sensors" thesis.
- Add **real ground observations** (OpenAQ / PurpleAir) → makes forecasting honest (target from a *different* source than features; defuses the §9.1 leakage problem).
- On-chain anchoring of validated readings (ties to your Track 2 blockchain+IoT research).
- Backfill historical data (Open-Meteo archive API, `archive-api.open-meteo.com`) for richer model training.
- Alerting via webhook / Telegram.

---

## 16. Deployment (local-first)

**v1 decision: local only.** The entire stack runs via `docker compose up` — broker, Postgres/Timescale, Spring Boot app, Python ML service, and the built React frontend. No cloud account, no credit card, no secrets beyond what compose injects.

Rationale:
- 2026 free tiers can't host this always-on (Fly.io/Koyeb dropped free compute; Railway is ~$1/mo credit; Render free web services spin down after ~15 min idle). Splitting across providers to dodge that is maintenance overhead with no portfolio payoff.
- For a public GitHub showcase, reviewers run it locally or just watch the demo. A reproducible `docker compose up` + an architecture diagram + a demo GIF in the README covers the assessment need.

What this means for the repo:
- The README's "Quickstart" is the deploy story: clone → `docker compose up` → open `localhost`.
- Record a short **demo GIF** of the live dashboard so the project is legible without running anything.
- Keep config env-driven (already required by G7) so a future host swap is trivial.

> **If you later want it live (v2, §17):** the cheapest honest path is *shrink + split* — managed MQTT (HiveMQ Cloud free), managed Postgres (Supabase/Neon, plain Postgres not Timescale), FE on Cloudflare Pages, Spring app on a free web service with a cron keep-alive ping, and in-JVM forecasting (drop the Python service, since it won't survive spin-down). Not needed for v1.

---

## 17. Licensing & Attribution (obligation, not optional)

- **Data:** Open-Meteo data is **CC BY 4.0** — attribution is *required*. The README and the dashboard footer must credit Open-Meteo (and CAMS as the upstream AQ model source). This is a license obligation, not a courtesy.
- **Code:** pick a permissive license for the repo (MIT/Apache-2.0) so it reads as a clean portfolio piece.
- **Honesty disclosures (README, prominent):** (1) data is modelled forecasts, not physical sensor readings; (2) "real-time" = pipeline reactivity, not data freshness; (3) the forecasting model is a surrogate/now-cast, not an attempt to beat CAMS. These three disclosures are what make the project read as *rigorous* rather than naive.
