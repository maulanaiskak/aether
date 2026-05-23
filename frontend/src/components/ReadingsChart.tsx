import {
  ComposedChart,
  Line,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  ReferenceLine,
  ResponsiveContainer,
} from 'recharts'
import type { SensorReadingDto, ForecastPointDto, AnomalyEventDto } from '../types/api'
import type { Metric } from './MetricSelector'

const METRIC_COLORS: Record<string, string> = {
  PM2_5: '#2196f3',
  PM10: '#ff9800',
  TEMPERATURE: '#f44336',
  HUMIDITY: '#4caf50',
}

interface ChartRow {
  ts: number
  [key: string]: number | null
}

function buildRows(readings: SensorReadingDto[], metrics: Set<Metric>): ChartRow[] {
  const map = new Map<number, ChartRow>()
  for (const r of readings) {
    if (!metrics.has(r.metric as Metric)) continue
    const ts = new Date(r.observedAt).getTime()
    const row = map.get(ts) ?? { ts }
    row[r.metric] = r.value
    map.set(ts, row)
  }
  return Array.from(map.values()).sort((a, b) => a.ts - b.ts)
}

function mergeForecast(rows: ChartRow[], forecast: ForecastPointDto[]): ChartRow[] {
  const extended = [...rows]
  for (const p of forecast) {
    const ts = new Date(p.horizonAt).getTime()
    extended.push({ ts, forecast_predicted: p.predicted, forecast_upper: p.upperBound, forecast_lower: p.lowerBound })
  }
  return extended.sort((a, b) => a.ts - b.ts)
}

interface Props {
  readings: SensorReadingDto[]
  forecast?: ForecastPointDto[]
  anomalies?: AnomalyEventDto[]
  metrics: Set<Metric>
  showForecast: boolean
  showAnomalies: boolean
}

export default function ReadingsChart({ readings, forecast, anomalies, metrics, showForecast, showAnomalies }: Props) {
  let data = buildRows(readings, metrics)
  if (showForecast && forecast?.length) data = mergeForecast(data, forecast)

  return (
    <ResponsiveContainer width="100%" height={260}>
      <ComposedChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        <XAxis
          dataKey="ts"
          type="number"
          scale="time"
          domain={['dataMin', 'dataMax']}
          tickFormatter={(t: number) => new Date(t).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          tick={{ fontSize: 11 }}
        />
        <YAxis tick={{ fontSize: 11 }} />
        <Tooltip
          labelFormatter={(t: number) => new Date(t).toLocaleString()}
          formatter={(v: number) => [v?.toFixed(2)]}
        />
        <Legend />

        {Array.from(metrics).map((m) => (
          <Line
            key={m}
            type="monotone"
            dataKey={m}
            stroke={METRIC_COLORS[m] ?? '#888'}
            dot={false}
            isAnimationActive={false}
            connectNulls
          />
        ))}

        {showForecast && forecast?.length && (
          <>
            <Area
              type="monotone"
              dataKey="forecast_upper"
              stroke="none"
              fill="#bbdefb"
              fillOpacity={0.5}
              isAnimationActive={false}
            />
            <Area
              type="monotone"
              dataKey="forecast_lower"
              stroke="none"
              fill="#ffffff"
              fillOpacity={1}
              isAnimationActive={false}
            />
            <Line
              type="monotone"
              dataKey="forecast_predicted"
              stroke="#1565c0"
              strokeDasharray="6 3"
              dot={false}
              isAnimationActive={false}
            />
          </>
        )}

        {showAnomalies &&
          anomalies?.map((a) => (
            <ReferenceLine
              key={`${a.id}-${a.detectedAt}`}
              x={new Date(a.detectedAt).getTime()}
              stroke="#f44336"
              strokeDasharray="3 3"
              label={{ value: `${a.method} ${a.score.toFixed(1)}`, position: 'top', fontSize: 9 }}
            />
          ))}
      </ComposedChart>
    </ResponsiveContainer>
  )
}
