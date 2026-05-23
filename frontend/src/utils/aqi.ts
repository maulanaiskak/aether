interface AqiLevel {
  max: number
  label: string
  color: string
}

const AQI_LEVELS: AqiLevel[] = [
  { max: 12,       label: 'Good',                           color: '#4caf50' },
  { max: 35.4,     label: 'Moderate',                       color: '#ffeb3b' },
  { max: 55.4,     label: 'Unhealthy for Sensitive Groups', color: '#ff9800' },
  { max: 150.4,    label: 'Unhealthy',                      color: '#f44336' },
  { max: 250.4,    label: 'Very Unhealthy',                 color: '#9c27b0' },
  { max: Infinity, label: 'Hazardous',                      color: '#7b1fa2' },
]

export function getAqiLevel(pm25: number): AqiLevel {
  return AQI_LEVELS.find((l) => pm25 <= l.max) ?? AQI_LEVELS[AQI_LEVELS.length - 1]
}
