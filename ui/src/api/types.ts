/**
 * API types must match REST DTOs from telemetry-processing-api-service:
 * SessionDto, LapResponseDto, SessionSummaryDto (see telemetry-api-contracts api/rest).
 */

export interface Session {
  /** Session public id (UUID). Use in URLs and WebSocket subscribe. */
  id: string
  /** User-facing display name (max 64 chars). Editable via PATCH. */
  sessionDisplayName?: string | null
  sessionType?: string | null
  trackId?: number | null
  trackLengthM?: number
  totalLaps?: number
  /** Player car index (0–19); use for laps/summary/tyre-wear so data is for the driver. */
  playerCarIndex?: number | null
  aiDifficulty?: number
  startedAt: string
  endedAt?: string | null
  endReason?: string | null
  state?: SessionState
  /** Finishing position (race position at session end). Null if active or no data. */
  finishingPosition?: number | null
}

export type SessionState = 'ACTIVE' | 'FINISHED'

export interface Lap {
  lapNumber: number
  lapTimeMs: number | null
  sector1Ms: number | null
  sector2Ms: number | null
  sector3Ms: number | null
  isInvalid: boolean
  /** Race position at the start of this lap. Optional (from API). */
  positionAtLapStart?: number | null
}

export interface SessionSummary {
  totalLaps: number
  bestLapTimeMs: number | null
  bestLapNumber: number | null
  bestSector1Ms: number | null
  bestSector2Ms: number | null
  bestSector3Ms: number | null
}

export interface ApiErrorBody {
  error: string
  message: string
}

export class HttpError extends Error {
  status: number
  body?: unknown

  constructor(status: number, message: string, body?: unknown) {
    super(message)
    this.status = status
    this.body = body
  }
}

