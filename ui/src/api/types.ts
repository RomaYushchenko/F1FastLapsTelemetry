/**
 * API types must match REST DTOs from telemetry-processing-api-service:
 * SessionDto, LapResponseDto, SessionSummaryDto (see telemetry-api-contracts api/rest).
 */

export interface Session {
  /** Session public id (UUID). Use in URLs and WebSocket subscribe. */
  id: string
  sessionType?: string | null
  trackId?: number | null
  trackLengthM?: number
  totalLaps?: number
  aiDifficulty?: number
  startedAt: string
  endedAt?: string | null
  endReason?: string | null
  state?: SessionState
}

export type SessionState = 'ACTIVE' | 'FINISHED'

export interface Lap {
  lapNumber: number
  lapTimeMs: number | null
  sector1Ms: number | null
  sector2Ms: number | null
  sector3Ms: number | null
  isInvalid: boolean
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

