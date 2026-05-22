# Task 25: Frontend Scaffold

**Status:** pending
**HLD Reference:** §Technical Implementation — Frontend stack, §Milestones M5

## Description

Scaffold the React + Vite + TypeScript frontend project. Configure TanStack Query for REST data fetching, set up the project directory structure, configure the Vite proxy for local dev (forward `/api` to `localhost:8080`), and implement the basic app shell (layout, location selector, empty page slots).

## Acceptance Criteria

- [ ] `frontend/` directory with `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`
- [ ] Dependencies: `react@19`, `react-dom@19`, `@tanstack/react-query`, `recharts`, TypeScript, Vite 6
- [ ] Vite dev proxy: `/api` → `http://localhost:8080`, `/stream` → `http://localhost:8080` (SSE)
- [ ] App shell: top nav with location selector (Surabaya / Jakarta / Bandung), main content area, footer with Open-Meteo attribution (CC BY 4.0 — required)
- [ ] TanStack Query `QueryClient` configured with `staleTime: 30_000`
- [ ] TypeScript types for `SensorReadingDto`, `AnomalyEventDto`, `ForecastPointDto`, `InsightResponseDto` matching backend DTO shapes
- [ ] `npm run build` succeeds with no TypeScript errors
- [ ] `frontend/Dockerfile`: nginx serving the Vite build output; `nginx.conf` that proxies `/api` and `/stream` to `aerator:8080`

## Dependencies

- **Depends on:** Task 21 (API endpoints to query), Task 22 (SSE endpoints to connect)
- **Blocks:** Task 26, 27, 28, 29

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `frontend/package.json` | Create | Dependencies + scripts |
| `frontend/vite.config.ts` | Create | Dev proxy config |
| `frontend/src/main.tsx` | Create | React root + QueryClientProvider |
| `frontend/src/App.tsx` | Create | App shell with location selector |
| `frontend/src/types/api.ts` | Create | TypeScript DTO types |
| `frontend/src/api/client.ts` | Create | TanStack Query hooks (useReadings, useForecast, useAnomalies) |
| `frontend/Dockerfile` | Create | Multi-stage: node build → nginx serve |
| `frontend/nginx.conf` | Create | Proxy /api and /stream to aerator |

## Implementation Hints

- **nginx.conf proxy for SSE (critical):**
  ```nginx
  location /stream/ {
      proxy_pass http://aerator:8080;
      proxy_http_version 1.1;
      proxy_set_header Connection '';      # keep-alive, not upgrade
      proxy_buffering off;                 # disable buffering — SSE requires immediate flush
      proxy_cache off;
      chunked_transfer_encoding on;
  }
  ```
  Without `proxy_buffering off`, nginx will buffer SSE events and the browser won't receive them in real time.
- **Attribution footer (license obligation):**
  ```tsx
  <footer>Data: <a href="https://open-meteo.com/">Open-Meteo</a> (CC BY 4.0) · AQ model: CAMS</footer>
  ```
- **Key consideration:** The Open-Meteo CC BY 4.0 attribution is a license obligation, not optional. It must appear in the dashboard UI and in the README.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
