export interface PacePoint {
  lapNumber: number
  lapTimeMs: number
  stintIndex?: number
  tyreCompound?: string
}

export interface PedalTracePoint {
  distance: number
  throttle: number
  brake: number
}

