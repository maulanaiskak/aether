# Task 27: Live Updates (SSE EventSource)

**Status:** pending
**HLD Reference:** §API Design — SSE reconnect strategy, §Functional Requirements FR-14

## Description

Integrate the SSE `EventSource` connection into the React dashboard. When new readings arrive via SSE, append them to the chart data in real time. Implement the reconnect strategy: on `EventSource` reconnect, call `/api/v1/readings/latest` to fetch missed readings before resubscribing to the stream.

## Acceptance Criteria

- [ ] `useReadingStream(location)` custom hook: opens `EventSource` to `/api/v1/stream/readings/{location}`; on `reading` event, updates chart data state
- [ ] `useAlertStream(location)` hook: opens `EventSource` to `/api/v1/stream/alerts/{location}`; on `alert` event, triggers a toast/badge notification
- [ ] On `EventSource` `onerror` + reconnect: call `GET /api/v1/readings/latest?location=` and merge results into chart data (gap fill from REST)
- [ ] `EventSource` closed when component unmounts (cleanup in `useEffect` return)
- [ ] AQI gauge updates in real-time when a new PM2.5 reading arrives via SSE
- [ ] Pipeline latency visually demonstrable: MQTT publish → chart update in < 5 s (in compose stack)
- [ ] Loading indicator while waiting for first SSE event

## Dependencies

- **Depends on:** Task 22 (SSE endpoints), Task 25 (scaffold), Task 26 (chart to update)
- **Blocks:** Task 28 (alert markers on chart)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `frontend/src/hooks/useReadingStream.ts` | Create | SSE EventSource hook for readings |
| `frontend/src/hooks/useAlertStream.ts` | Create | SSE EventSource hook for alerts |
| `frontend/src/components/LiveIndicator.tsx` | Create | "Live" badge / pulsing dot |

## Implementation Hints

- **EventSource hook:**
  ```ts
  export function useReadingStream(location: string, onReading: (r: SensorReadingDto) => void) {
    useEffect(() => {
      const es = new EventSource(`/api/v1/stream/readings/${location}`);
      es.addEventListener('reading', (e) => onReading(JSON.parse(e.data)));
      es.onerror = () => {
        // reconnect is automatic; fetch latest to fill gap
        fetchLatestReadings(location).then(onReading);
      };
      return () => es.close();
    }, [location]);
  }
  ```
- **State merge on reconnect:** append new readings from `/readings/latest` to the existing chart data array; deduplicate by `observedAt`.
- **Key consideration:** `EventSource` does not support custom headers (no Authorization). This is fine for v1 (no auth). If auth is added in v2, switch to `fetch()` with `ReadableStream` or use a library like `@microsoft/fetch-event-source`.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
