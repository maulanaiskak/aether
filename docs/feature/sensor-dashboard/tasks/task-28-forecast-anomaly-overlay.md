# Task 28: Forecast Overlay + Anomaly Markers

**Status:** pending
**HLD Reference:** §Functional Requirements FR-15, FR-19, FR-20

## Description

Extend `ReadingsChart` with a forecast overlay (predicted line + confidence band as shaded `ReferenceArea`) and anomaly markers (vertical `ReferenceLine` or dot markers on the chart at anomalous timestamps). Data fetched via TanStack Query.

## Acceptance Criteria

- [ ] `ForecastOverlay` renders predicted PM2.5 as a dashed line extending beyond the current time
- [ ] Confidence band rendered as a shaded area between `lowerBound` and `upperBound`
- [ ] Anomaly markers: vertical dashed line or custom dot at each anomalous `observedAt` timestamp; tooltip shows method + score
- [ ] Toggle buttons to show/hide forecast and anomaly overlays independently
- [ ] `useForecast(location)` hook: `GET /api/v1/forecast?location=&metric=PM2_5&hours=24`
- [ ] `useAnomalies(location)` hook: `GET /api/v1/anomalies?location=&metric=PM2_5&from=now-24h`
- [ ] Recharts `ComposedChart` used (supports mixing `Line`, `Area`, `ReferenceLine`)

## Dependencies

- **Depends on:** Task 18 (forecast data in DB), Task 26 (base chart), Task 27 (live updates pipe)
- **Blocks:** Task 34 (demo GIF recorded from this final state)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `frontend/src/components/ForecastOverlay.tsx` | Create | Forecast line + confidence band |
| `frontend/src/components/AnomalyMarkers.tsx` | Create | Anomaly reference lines |
| `frontend/src/hooks/useForecast.ts` | Create | TanStack Query hook |
| `frontend/src/hooks/useAnomalies.ts` | Create | TanStack Query hook |
| `frontend/src/components/ReadingsChart.tsx` | Modify | Integrate overlays into ComposedChart |

## Implementation Hints

- **Recharts confidence band:**
  ```tsx
  <Area type="monotone" dataKey="upper" stroke="none" fill="#bbdefb" fillOpacity={0.4} />
  <Area type="monotone" dataKey="lower" stroke="none" fill="#ffffff" fillOpacity={1} />
  <Line type="monotone" dataKey="predicted" stroke="#1565c0" strokeDasharray="6 3" />
  ```
  Stack two Areas: fill the upper to `#bbdefb`, then overlay the lower filled white to carve out the band.
- **Anomaly markers:**
  ```tsx
  {anomalies.map(a => (
    <ReferenceLine key={a.observedAt} x={a.observedAt} stroke="#f44336"
      strokeDasharray="3 3"
      label={{ value: `${a.method} ${a.score.toFixed(1)}`, position: 'top', fontSize: 10 }} />
  ))}
  ```

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
