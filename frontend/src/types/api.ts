export type QualityStatus = 'OK' | 'SUSPECT' | 'REJECTED'
export type QualityFlag = 'MISSING_VALUE' | 'OUT_OF_RANGE' | 'STALE' | 'IMPUTED' | 'ANOMALOUS'

export interface Quality {
  status: QualityStatus
  flags: QualityFlag[]
}

export interface SensorReadingDto {
  sensorId: string
  location: string
  metric: string
  unit: string
  value: number | null
  observedAt: string
  quality: Quality
}

export interface AnomalyEventDto {
  id: string
  sensorId: string
  location: string
  metric: string
  value: number
  score: number
  method: string
  detectedAt: string
}

export interface ForecastPointDto {
  horizonAt: string
  predicted: number
  lowerBound: number
  upperBound: number
}

export interface ForecastResponseDto {
  location: string
  metric: string
  model: string
  generatedAt: string
  points: ForecastPointDto[]
}

export interface InsightRequestDto {
  location: string
  metric: string
  windowHours: number
}

export interface InsightResponseDto {
  location: string
  metric: string
  provider: string
  insights: InsightDto[]
  generatedAt: string
}

export interface InsightDto {
  type: string
  severity: string
  title: string
  description: string
}

export interface PageDto<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
