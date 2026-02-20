export interface WsSnapshotMessage {
  type: 'SNAPSHOT'
  timestamp: string
  speedKph: number | null
  gear: number | null
  engineRpm: number | null
  throttle: number | null
  brake: number | null
  drs: boolean | null
  currentLap: number | null
  currentSector: number | null
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

