import { useEffect } from 'react'
import type { AnomalyEventDto } from '../types/api'

export function useAlertStream(location: string, onAlert: (a: AnomalyEventDto) => void) {
  useEffect(() => {
    const es = new EventSource(`/api/v1/stream/alerts/${location}`)
    es.addEventListener('alert', (e: MessageEvent) => {
      try {
        onAlert(JSON.parse(e.data) as AnomalyEventDto)
      } catch {
        // malformed event — ignore
      }
    })
    return () => es.close()
  }, [location, onAlert])
}
