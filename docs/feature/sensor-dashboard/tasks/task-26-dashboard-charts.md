# Task 26: Dashboard Charts + AQI Gauge

**Status:** pending
**HLD Reference:** §Functional Requirements FR-15, FR-17 (React dashboard)

## Description

Implement the historical data visualization components: a multi-metric time-series `LineChart` (Recharts) for the selected location, and an AQI gauge that shows current air quality status with color-coded label. Data is fetched via TanStack Query from `GET /api/v1/readings` and `GET /api/v1/readings/latest`.

## Acceptance Criteria

- [ ] `ReadingsChart` component: Recharts `LineChart` rendering last 24h for selected location; one line per metric; `smoothedValue` shown as dashed secondary line if available
- [ ] Metric selector (checkboxes or tabs): toggle PM2.5, PM10, temperature, humidity
- [ ] `AqiGauge` component: shows current PM2.5 value with AQI category label and color (Good=green, Moderate=yellow, Unhealthy=orange, etc.)
- [ ] Loading skeleton shown while TanStack Query fetches; error state shown on fetch failure
- [ ] Timestamps displayed in local time (browser timezone); backend sends UTC — frontend converts
- [ ] `npm run build` with no TS errors after this task

## Dependencies

- **Depends on:** Task 25 (scaffold, API hooks)
- **Blocks:** Task 27 (live updates extend these charts), Task 28 (forecast overlay added to chart)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `frontend/src/components/ReadingsChart.tsx` | Create | Multi-metric time-series chart |
| `frontend/src/components/AqiGauge.tsx` | Create | Current AQI value + label |
| `frontend/src/components/MetricSelector.tsx` | Create | Metric toggle UI |
| `frontend/src/hooks/useReadings.ts` | Create | TanStack Query hook for readings |
| `frontend/src/utils/aqi.ts` | Create | PM2.5 → AQI label + color |

## Implementation Hints

- **Recharts time-series:**
  ```tsx
  <LineChart data={readings}>
    <XAxis dataKey="observedAt" tickFormatter={t => new Date(t).toLocaleTimeString()} />
    <YAxis />
    <Tooltip />
    <Line type="monotone" dataKey="value" stroke="#2196f3" dot={false} />
    <Line type="monotone" dataKey="smoothedValue" stroke="#90caf9" strokeDasharray="4 2" dot={false} />
  </LineChart>
  ```
- **AQI color mapping:**
  ```ts
  const AQI_LEVELS = [
    { max: 12,    label: 'Good',                          color: '#4caf50' },
    { max: 35.4,  label: 'Moderate',                      color: '#ffeb3b' },
    { max: 55.4,  label: 'Unhealthy for Sensitive Groups', color: '#ff9800' },
    { max: 150.4, label: 'Unhealthy',                     color: '#f44336' },
    { max: 250.4, label: 'Very Unhealthy',                color: '#9c27b0' },
    { max: Infinity, label: 'Hazardous',                  color: '#7b1fa2' },
  ];
  ```

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
