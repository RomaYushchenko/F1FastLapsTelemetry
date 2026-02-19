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

export interface TyreWearPoint {
  lapNumber: number
  wearFL: number | null
  wearFR: number | null
  wearRL: number | null
  wearRR: number | null
}

