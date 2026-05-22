# Task 34: Docker Compose Validation + CI + README

**Status:** pending
**HLD Reference:** §Milestones M8 — Polish, §Deployment Plan, §Licensing & Attribution

## Description

Final polish task: validate the full compose stack works end-to-end on a clean machine, set up GitHub Actions CI, write the README with architecture diagram, honest disclosures, and demo GIF. This is the M8 gate — the project is only "done" when this passes.

## Acceptance Criteria

- [ ] `docker compose up` on a fresh clone → all services healthy, dashboard accessible at `http://localhost` within 3 minutes
- [ ] `docker compose up` produces no manual configuration prompts — `.env.example` defaults work
- [ ] GitHub Actions CI (`.github/workflows/ci.yml`) runs: `./gradlew build test` + `docker build .` + `docker build ml-service/` + `docker build frontend/`
- [ ] CI passes on push to `main`
- [ ] `README.md` contains:
  - Project description + honesty disclosures (3 required per HLD §17)
  - Architecture diagram (copy from HLD or render as image)
  - Quickstart: `git clone → docker compose up → open localhost`
  - MQTT topic map
  - Open-Meteo CC BY 4.0 attribution (license obligation)
  - CAMS model attribution
  - Link to `docs/feature/sensor-dashboard/HLD-sensor-dashboard.md`
- [ ] Demo GIF recorded showing: live chart updating, anomaly marker appearing, "Explain this" insight panel

## Dependencies

- **Depends on:** All previous tasks (34 is the integration gate)
- **Blocks:** Nothing (final task)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `.github/workflows/ci.yml` | Create | GitHub Actions pipeline |
| `README.md` | Create | Project documentation |
| `docs/demo.gif` | Create | Screen recording of live dashboard |

## Implementation Hints

- **Required honesty disclosures (verbatim from HLD §17):**
  1. "Data is CAMS modelled forecasts, not physical sensor readings."
  2. "'Real-time' describes pipeline reactivity (< 5 s MQTT → browser), not data freshness (hourly upstream)."
  3. "The PM2.5 forecast is a cheap surrogate/now-cast over the canonical pipeline, not an attempt to out-predict CAMS."
- **CI workflow:**
  ```yaml
  name: CI
  on: [push, pull_request]
  jobs:
    build:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with: { java-version: '21', distribution: 'temurin' }
        - run: ./gradlew build test --no-daemon
        - run: docker build -t aerator .
        - run: docker build -t ml-service ./ml-service
        - run: docker build -t frontend ./frontend
  ```
- **GIF recording:** Use `ffmpeg` or a tool like LICEcap/Kap. Capture: chart updating live, an anomaly appearing as a marker, and the insight panel opening.
- **MQTT topic map (in README):**
  ```
  sensors/{location}/{metric}   — raw sensor readings (published by ingestion)
  alerts/{location}/{metric}    — anomaly events (published by api)
  aether/system/heartbeat       — 30s health pulse
  ```
- **Key consideration:** The demo GIF is what most reviewers see before they even read the code. Record it last, when all features are working, and make sure it shows the pipeline latency claim visibly.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
