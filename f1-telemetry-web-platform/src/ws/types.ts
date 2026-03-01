/**
 * WebSocket message types for live telemetry.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 4.5.
 */

/** Live snapshot (10 Hz). Order for tyresSurfaceTempC: RL, RR, FL, FR. */
export interface WsSnapshotMessage {
  type: "SNAPSHOT";
  timestamp?: string | null;
  speedKph?: number | null;
  gear?: number | null;
  engineRpm?: number | null;
  throttle?: number | null;
  brake?: number | null;
  drs?: boolean | null;
  drsAllowed?: boolean | null;
  currentLap?: number | null;
  currentSector?: number | null;
  currentSectorDisplayName?: string | null;
  currentLapTimeMs?: number | null;
  bestLapTimeMs?: number | null;
  deltaMs?: number | null;
  ersEnergyPercent?: number | null;
  ersDeployActive?: boolean | null;
  ersDeployModeDisplayName?: string | null;
  /** Tyre surface temperatures °C, order RL, RR, FL, FR. */
  tyresSurfaceTempC?: number[] | null;
  /** Fuel remaining 0–100%. */
  fuelRemainingPercent?: number | null;
}

export interface WsSessionEndedMessage {
  type: "SESSION_ENDED";
  sessionId: string;
  endReason: string;
}

export interface WsErrorMessage {
  type: "ERROR";
  message?: string;
}

export type WsServerMessage =
  | WsSnapshotMessage
  | WsSessionEndedMessage
  | WsErrorMessage;

/** Tyre position labels; index order matches tyresSurfaceTempC: RL, RR, FL, FR. */
export const TYRE_LABELS = ["RL", "RR", "FL", "FR"] as const;
