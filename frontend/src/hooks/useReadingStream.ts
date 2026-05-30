import { useEffect } from 'react'
import type { SensorReadingDto } from '../types/api'

export function useReadingStream(location: string, onReading: (r: SensorReadingDto) => void) {
  useEffect(() => {
    const es = new EventSource(`/api/v1/stream/readings/${location.toLowerCase()}`)
    es.addEventListener('reading', (e: MessageEvent) => {
      try {
        onReading(JSON.parse(e.data) as SensorReadingDto)
      } catch {
        // malformed event — ignore
      }
    })
    es.onerror = () => {
      fetch(`/api/v1/readings/latest?location=${location.toLowerCase()}`)
        .then((r) => (r.ok ? r.json() : null))
        .then((data) => {
          if (Array.isArray(data)) data.forEach(onReading)
        })
        .catch(() => undefined)
    }
    return () => es.close()
  }, [location, onReading])
}
