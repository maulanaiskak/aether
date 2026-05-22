# Task 29: Insight Panel

**Status:** pending
**HLD Reference:** §Functional Requirements FR-16, FR-21, §Technical Implementation — Insight Engine

## Description

Add the "Explain this" button and insight modal/panel to the dashboard. On click, the frontend calls `POST /api/v1/insight` with the current location and timestamp, then displays the returned summary. Show a loading spinner during the POST call.

## Acceptance Criteria

- [ ] "Explain this" button visible in the dashboard header or below the chart
- [ ] On click: `POST /api/v1/insight { location, asOf: now().toISOString() }`
- [ ] Loading state shown during request (button disabled + spinner)
- [ ] Result displayed in a panel/modal: summary text + "Powered by rule-based engine" label
- [ ] Error state: if POST fails, show "Could not generate insight — try again" with retry button
- [ ] `provider` field in response shown as a small badge (e.g. "rule-based")

## Dependencies

- **Depends on:** Task 23 (insight endpoint), Task 26 (chart + location context)
- **Blocks:** Task 34 (demo GIF)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `frontend/src/components/InsightPanel.tsx` | Create | Panel with summary text + provider badge |
| `frontend/src/components/ExplainButton.tsx` | Create | Button that triggers insight POST |
| `frontend/src/hooks/useInsight.ts` | Create | TanStack Query mutation for POST /insight |

## Implementation Hints

- **TanStack Query mutation:**
  ```ts
  const mutation = useMutation({
    mutationFn: (req: InsightRequestDto) =>
      fetch('/api/v1/insight', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req),
      }).then(r => r.json() as Promise<InsightResponseDto>),
  });
  ```
- **Panel:**
  ```tsx
  {mutation.data && (
    <div className="insight-panel">
      <p>{mutation.data.summary}</p>
      <span className="badge">{mutation.data.provider}</span>
    </div>
  )}
  ```

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
