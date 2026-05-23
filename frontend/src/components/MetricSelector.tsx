const METRICS = ['PM2_5', 'PM10', 'TEMPERATURE', 'HUMIDITY'] as const
export type Metric = (typeof METRICS)[number]

const LABELS: Record<Metric, string> = {
  PM2_5: 'PM2.5',
  PM10: 'PM10',
  TEMPERATURE: 'Temp',
  HUMIDITY: 'Humidity',
}

interface Props {
  selected: Set<Metric>
  onChange: (m: Set<Metric>) => void
}

export default function MetricSelector({ selected, onChange }: Props) {
  function toggle(m: Metric) {
    const next = new Set(selected)
    if (next.has(m)) {
      if (next.size > 1) next.delete(m)
    } else {
      next.add(m)
    }
    onChange(next)
  }

  return (
    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
      {METRICS.map((m) => (
        <label key={m} style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.875rem', cursor: 'pointer' }}>
          <input type="checkbox" checked={selected.has(m)} onChange={() => toggle(m)} />
          {LABELS[m]}
        </label>
      ))}
    </div>
  )
}
