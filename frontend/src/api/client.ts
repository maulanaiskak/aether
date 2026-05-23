import { useQuery, useMutation } from '@tanstack/react-query'
import type {
  SensorReadingDto,
  AnomalyEventDto,
  ForecastResponseDto,
  InsightRequestDto,
  InsightResponseDto,
  PageDto,
} from '../types/api'

const BASE = '/api/v1'

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json() as Promise<T>
}

export function useReadings(location: string, metric?: string, limit = 48) {
  const params = new URLSearchParams({ location, limit: String(limit) })
  if (metric) params.set('metric', metric)
  return useQuery<PageDto<SensorReadingDto>>({
    queryKey: ['readings', location, metric, limit],
    queryFn: () => get(`/readings?${params}`),
    staleTime: 30_000,
  })
}

export function useForecast(location: string, metric = 'PM2_5') {
  return useQuery<ForecastResponseDto>({
    queryKey: ['forecast', location, metric],
    queryFn: () => get(`/forecast?location=${location}&metric=${metric}`),
    staleTime: 30_000,
  })
}

export function useAnomalies(location: string, limit = 20) {
  return useQuery<PageDto<AnomalyEventDto>>({
    queryKey: ['anomalies', location, limit],
    queryFn: () => get(`/anomalies?location=${location}&limit=${limit}`),
    staleTime: 30_000,
  })
}

export function useInsight() {
  return useMutation<InsightResponseDto, Error, InsightRequestDto>({
    mutationFn: (req) => post('/insight', req),
  })
}
