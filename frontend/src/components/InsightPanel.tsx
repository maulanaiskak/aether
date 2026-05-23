import type { InsightResponseDto } from '../types/api'

interface Props {
  data: InsightResponseDto | null
  isLoading: boolean
  error: Error | null
  onRetry: () => void
}

export default function InsightPanel({ data, isLoading, error, onRetry }: Props) {
  if (isLoading) {
    return <div style={{ padding: '1rem', color: '#64748b' }}>Generating insight…</div>
  }
  if (error) {
    return (
      <div style={{ padding: '1rem' }}>
        <span style={{ color: '#ef4444' }}>Could not generate insight — try again</span>
        <button onClick={onRetry} style={{ marginLeft: '0.75rem', cursor: 'pointer' }}>Retry</button>
      </div>
    )
  }
  if (!data) return null

  return (
    <div style={{ padding: '1rem', background: '#f0f9ff', borderRadius: '6px', border: '1px solid #bae6fd' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
        <span style={{ fontWeight: 600, color: '#0369a1' }}>AI Insights — {data.location}</span>
        <span style={{ fontSize: '0.7rem', background: '#e0f2fe', color: '#0369a1', borderRadius: '4px', padding: '2px 6px' }}>
          {data.provider}
        </span>
      </div>
      <ul style={{ margin: 0, padding: '0 0 0 1rem', listStyle: 'disc' }}>
        {data.insights.map((ins, i) => (
          <li key={i} style={{ marginBottom: '0.25rem', fontSize: '0.875rem', color: '#1e3a5f' }}>
            <strong>{ins.title}</strong>: {ins.description}
          </li>
        ))}
      </ul>
    </div>
  )
}
