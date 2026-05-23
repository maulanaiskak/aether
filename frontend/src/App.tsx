import { useState, useCallback, useRef } from 'react'
import { useReadings, useForecast, useAnomalies, useInsight } from './api/client'
import ReadingsChart from './components/ReadingsChart'
import AqiGauge from './components/AqiGauge'
import MetricSelector, { type Metric } from './components/MetricSelector'
import LiveIndicator from './components/LiveIndicator'
import InsightPanel from './components/InsightPanel'
import { useReadingStream } from './hooks/useReadingStream'
import type { SensorReadingDto } from './types/api'

const LOCATIONS = ['Surabaya', 'Jakarta', 'Bandung'] as const
type Location = (typeof LOCATIONS)[number]

export default function App() {
  const [location, setLocation] = useState<Location>('Surabaya')
  const [metrics, setMetrics] = useState<Set<Metric>>(new Set(['PM2_5']))
  const [showForecast, setShowForecast] = useState(true)
  const [showAnomalies, setShowAnomalies] = useState(true)
  const [liveReadings, setLiveReadings] = useState<SensorReadingDto[]>([])
  const [isLive, setIsLive] = useState(false)
  const seenRef = useRef(new Set<string>())

  const { data: readingsPage, isLoading: readingsLoading } = useReadings(location, undefined, 48)
  const { data: forecast } = useForecast(location)
  const { data: anomaliesPage } = useAnomalies(location)
  const insightMutation = useInsight()

  const onReading = useCallback((r: SensorReadingDto) => {
    const key = `${r.sensorId}-${r.observedAt}`
    if (seenRef.current.has(key)) return
    seenRef.current.add(key)
    setIsLive(true)
    setLiveReadings((prev) => [...prev.slice(-200), r])
  }, [])

  useReadingStream(location, onReading)

  const allReadings = [
    ...(readingsPage?.content ?? []),
    ...liveReadings,
  ]

  const latestPm25 = allReadings
    .filter((r) => r.metric === 'PM2_5')
    .sort((a, b) => b.observedAt.localeCompare(a.observedAt))[0]?.value ?? null

  function requestInsight() {
    insightMutation.mutate({ location, metric: 'PM2_5', windowHours: 24 })
  }

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={{ background: '#1e293b', color: '#f1f5f9', padding: '0 1.5rem', display: 'flex', alignItems: 'center', gap: '1.5rem', height: '56px' }}>
        <span style={{ fontWeight: 700, fontSize: '1.1rem', letterSpacing: '0.05em' }}>AETHER</span>
        <nav style={{ display: 'flex', gap: '0.5rem' }}>
          {LOCATIONS.map((loc) => (
            <button
              key={loc}
              onClick={() => { setLocation(loc); setLiveReadings([]); seenRef.current.clear(); setIsLive(false) }}
              style={{
                background: loc === location ? '#3b82f6' : 'transparent',
                color: '#f1f5f9',
                border: '1px solid',
                borderColor: loc === location ? '#3b82f6' : '#475569',
                borderRadius: '4px',
                padding: '0.25rem 0.75rem',
                cursor: 'pointer',
                fontSize: '0.875rem',
              }}
            >
              {loc}
            </button>
          ))}
        </nav>
        <div style={{ marginLeft: 'auto' }}>
          <LiveIndicator live={isLive} />
        </div>
      </header>

      <main style={{ flex: 1, padding: '1.5rem', background: '#f8fafc' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
          <h1 style={{ margin: 0, fontSize: '1.25rem', color: '#0f172a' }}>Air Quality — {location}</h1>
          <AqiGauge pm25={latestPm25} />
        </div>

        <div style={{ background: '#fff', borderRadius: '8px', padding: '1rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <MetricSelector selected={metrics} onChange={setMetrics} />
            <div style={{ display: 'flex', gap: '0.75rem', fontSize: '0.875rem' }}>
              <label style={{ cursor: 'pointer' }}>
                <input type="checkbox" checked={showForecast} onChange={(e) => setShowForecast(e.target.checked)} /> Forecast
              </label>
              <label style={{ cursor: 'pointer' }}>
                <input type="checkbox" checked={showAnomalies} onChange={(e) => setShowAnomalies(e.target.checked)} /> Anomalies
              </label>
            </div>
          </div>
          {readingsLoading ? (
            <div style={{ height: 260, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8' }}>Loading…</div>
          ) : (
            <ReadingsChart
              readings={allReadings}
              forecast={forecast?.points}
              anomalies={anomaliesPage?.content}
              metrics={metrics}
              showForecast={showForecast}
              showAnomalies={showAnomalies}
            />
          )}
        </div>

        <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 260 }}>
            <button
              onClick={requestInsight}
              disabled={insightMutation.isPending}
              style={{
                background: '#3b82f6',
                color: '#fff',
                border: 'none',
                borderRadius: '6px',
                padding: '0.5rem 1.25rem',
                cursor: insightMutation.isPending ? 'not-allowed' : 'pointer',
                opacity: insightMutation.isPending ? 0.7 : 1,
                fontSize: '0.875rem',
                marginBottom: '0.75rem',
              }}
            >
              {insightMutation.isPending ? 'Thinking…' : 'Explain this'}
            </button>
            <InsightPanel
              data={insightMutation.data ?? null}
              isLoading={insightMutation.isPending}
              error={insightMutation.error ?? null}
              onRetry={requestInsight}
            />
          </div>
        </div>
      </main>

      <footer style={{ background: '#1e293b', color: '#94a3b8', padding: '0.5rem 1.5rem', fontSize: '0.75rem' }}>
        Data: <a href="https://open-meteo.com/" style={{ color: '#60a5fa' }}>Open-Meteo</a> (CC BY 4.0) · AQ model: CAMS
      </footer>
    </div>
  )
}
