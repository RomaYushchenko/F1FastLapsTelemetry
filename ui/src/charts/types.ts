/** Matches PacePointDto from GET /api/sessions/{id}/pace. */
export interface PacePoint {
  lapNumber: number
  lapTimeMs: number
  stintIndex?: number
  tyreCompound?: string
}

/** Matches TracePointDto from GET /api/sessions/{id}/laps/{lapNum}/trace. */
export interface PedalTracePoint {
  distance: number
  throttle: number
  brake: number
}

/** Matches ErsPointDto from GET /api/sessions/{id}/laps/{lapNum}/ers. */
export interface ErsPoint {
  lapDistanceM: number
  energyPercent: number
}

/** Matches SpeedTracePointDto from GET /api/sessions/{id}/laps/{lapNum}/speed-trace. */
export interface SpeedTracePoint {
  distanceM: number
  speedKph: number
}

/** Matches TyreWearPointDto from GET /api/sessions/{id}/tyre-wear. */
export interface TyreWearPoint {
  lapNumber: number
  wearFL: number | null
  wearFR: number | null
  wearRL: number | null
  wearRR: number | null
  /** F1 25 actual tyre compound code (e.g. 16=C5 soft, 18=C3 medium). Optional until API provides it. */
  compound?: number | null
}

