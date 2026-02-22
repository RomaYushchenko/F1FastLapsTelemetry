export interface WsSnapshotMessage {
  type: 'SNAPSHOT'
  timestamp: string
  speedKph: number | null
  gear: number | null
  engineRpm: number | null
  throttle: number | null
  brake: number | null
  /** DRS wing open (from Car Telemetry). Plan 12. */
  drs: boolean | null
  /** DRS zone allowed (from Car Status). Optional "DRS available" when in zone. Plan 12. */
  drsAllowed?: boolean | null
  currentLap: number | null
  currentSector: number | null
  /** Human-readable sector (e.g. "Sector 1") from backend. Plan 10. */
  currentSectorDisplayName?: string | null
  /** Current lap time in ms. Used for delta to best. */
  currentLapTimeMs?: number | null
  /** Best lap time in session (ms). */
  bestLapTimeMs?: number | null
  /** Delta to best lap in ms (current − best). Negative = faster than best. */
  deltaMs?: number | null
  /** ERS energy store 0–100%. */
  ersEnergyPercent?: number | null
  /** ERS deploy active (driver using ERS). */
  ersDeployActive?: boolean | null
  /** ERS deploy mode label (e.g. "Hotlap", "Overtake"). Plan 11. */
  ersDeployModeDisplayName?: string | null
}

export interface WsSessionEndedMessage {
  type: 'SESSION_ENDED'
  sessionId: string
  endReason: string
}

export interface WsErrorMessage {
  type: 'ERROR'
  code: string
  message: string
}

export type WsServerMessage = WsSnapshotMessage | WsSessionEndedMessage | WsErrorMessage

