import { getAqiLevel } from '../utils/aqi'

interface Props {
  pm25: number | null
}

export default function AqiGauge({ pm25 }: Props) {
  if (pm25 === null) {
    return <div style={{ padding: '1rem', color: '#94a3b8' }}>No data</div>
  }
  const level = getAqiLevel(pm25)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', padding: '0.5rem 0' }}>
      <div
        style={{
          width: 64,
          height: 64,
          borderRadius: '50%',
          background: level.color,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 700,
          color: '#fff',
          fontSize: '1rem',
        }}
      >
        {pm25.toFixed(1)}
      </div>
      <div>
        <div style={{ fontWeight: 600, color: level.color }}>{level.label}</div>
        <div style={{ fontSize: '0.75rem', color: '#64748b' }}>PM2.5 µg/m³</div>
      </div>
    </div>
  )
}
