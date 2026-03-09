/**
 * API types aligned with REST contract (telemetry-processing-api-service).
 * SessionDto, LapResponseDto, SessionSummaryDto, chart DTOs.
 */

export interface Session {
  id: string
  sessionDisplayName?: string | null
  sessionType?: string | null
  trackId?: number | null
  trackDisplayName?: string | null
  trackLengthM?: number
  totalLaps?: number
  playerCarIndex?: number | null
  aiDifficulty?: number
  startedAt: string
  endedAt?: string | null
  endReason?: string | null
  state?: SessionState
  finishingPosition?: number | null
  /** Optional from list when backend adds it. */
  bestLapTimeMs?: number | null
  /** Optional from list when backend adds it. */
  totalTimeMs?: number | null
  /** Participants (car indices with data) for Driver Comparison. From GET /api/sessions/{id}. */
  participants?: SessionParticipantDto[] | null
}

/** Session participant for Driver A/B dropdowns (GET /api/sessions/{id}.participants). */
export interface SessionParticipantDto {
  carIndex: number
  displayLabel?: string | null
}

/** GET /api/sessions/{sessionUid}/comparison — Driver Comparison response. */
export interface ComparisonResponseDto {
  sessionUid: string
  carIndexA: number
  carIndexB: number
  lapsA: Lap[]
  lapsB: Lap[]
  summaryA: SessionSummary
  summaryB: SessionSummary
  paceA: PacePoint[]
  paceB: PacePoint[]
  referenceLapNumA: number
  referenceLapNumB: number
  traceA: PedalTracePoint[]
  traceB: PedalTracePoint[]
  speedTraceA: SpeedTracePoint[]
  speedTraceB: SpeedTracePoint[]
}

export type SessionState = 'ACTIVE' | 'FINISHED'

export interface Lap {
  lapNumber: number
  lapTimeMs: number | null
  sector1Ms: number | null
  sector2Ms: number | null
  sector3Ms: number | null
  isInvalid: boolean
  positionAtLapStart?: number | null
}

export interface SessionSummary {
  totalLaps: number
  bestLapTimeMs: number | null
  bestLapNumber: number | null
  bestSector1Ms: number | null
  bestSector2Ms: number | null
  bestSector3Ms: number | null
  leaderCarIndex?: number | null
  leaderIsPlayer?: boolean | null
}

export interface ApiErrorBody {
  error?: string
  message: string
}

export class HttpError extends Error {
  status: number
  body?: unknown

  constructor(status: number, message: string, body?: unknown) {
    super(message)
    this.status = status
    this.body = body
    Object.setPrototypeOf(this, HttpError.prototype)
  }
}

/** GET /api/sessions/{id}/pace */
export interface PacePoint {
  lapNumber: number
  lapTimeMs: number
}

/** GET /api/sessions/{id}/laps/{lapNum}/trace */
export interface PedalTracePoint {
  distance: number
  throttle: number
  brake: number
}

/** GET /api/sessions/{id}/laps/{lapNum}/ers */
export interface ErsPoint {
  lapDistanceM: number
  energyPercent: number
}

/** GET /api/sessions/{id}/laps/{lapNum}/speed-trace */
export interface SpeedTracePoint {
  distanceM: number
  speedKph: number
}

/** GET /api/sessions/{id}/laps/{lapNum}/corners */
export interface LapCorner {
  cornerIndex: number
  startDistanceM: number
  endDistanceM: number
  apexDistanceM: number
  entrySpeedKph: number
  apexSpeedKph: number
  exitSpeedKph: number
  durationMs?: number | null
  name?: string | null
}

/** GET /api/sessions/{id}/tyre-wear */
export interface TyreWearPoint {
  lapNumber: number
  wearFL: number | null
  wearFR: number | null
  wearRL: number | null
  wearRR: number | null
  compound?: string | number | null
}

/** GET /api/sessions/{id}/pit-stops */
export interface PitStopDto {
  lapNumber: number
  inLapTimeMs: number | null
  pitDurationMs: number | null
  outLapTimeMs: number | null
  compoundIn: number | null
  compoundOut: number | null
}

/** GET /api/sessions/{id}/stints */
export interface StintDto {
  stintIndex: number
  compound: number | null
  startLap: number
  lapCount: number
  avgLapTimeMs: number | null
  degradationIndicator: 'high' | 'medium' | 'low' | null
}

/** GET /api/sessions/{id}/fuel-by-lap — fuel at lap end (B6). */
export interface FuelByLapDto {
  lapNumber: number
  fuelKg: number | null
}

/** GET /api/sessions/{id}/ers-by-lap — ERS store % at lap end (B7). */
export interface ErsByLapDto {
  lapNumber: number
  ersStorePercentEnd: number | null
}

/** GET /api/sessions/active/leaderboard — live leaderboard entry */
export interface LeaderboardEntry {
  position: number
  carIndex: number
  driverLabel: string | null
  compound: string | null
  gap: string | null
  lastLapTimeMs: number | null
  sector1Ms: number | null
  sector2Ms: number | null
  sector3Ms: number | null
}

/** GET /api/sessions/{id}/events — session event for timeline */
export interface SessionEventDto {
  lap: number | null
  eventCode: string
  carIndex: number | null
  detail: Record<string, unknown> | null
  createdAt: string
}

/** GET /api/tracks/{trackId}/layout — 3D track map points and optional bounds */
export interface TrackPoint3D {
  x: number   // worldPositionX (horizontal)
  y: number   // worldPositionY (elevation)
  z: number   // worldPositionZ (horizontal depth)
}

export interface TrackBounds {
  minX: number
  maxX: number
  minZ: number
  maxZ: number
  minElev: number
  maxElev: number
}

export interface SectorBoundary {
  sector: 1 | 2 | 3
  x: number
  y: number
  z: number
}

export interface TrackLayoutResponseDto {
  trackId: number
  points: TrackPoint3D[]
  bounds?: TrackBounds
  source?: 'STATIC' | 'RECORDED' | string
  sectorBoundaries?: SectorBoundary[] | null
}

export interface TrackLayoutStatusDto {
  trackId: number
  status: 'READY' | 'RECORDING' | 'NOT_AVAILABLE'
  pointsCollected: number
  source?: 'STATIC' | 'RECORDED' | string | null
}

/** GET /api/sessions/active/positions or WebSocket POSITIONS — one car's world position (B9). */
export interface CarPositionDto {
  carIndex: number
  worldPosX: number
  worldPosY: number
  worldPosZ: number
}
