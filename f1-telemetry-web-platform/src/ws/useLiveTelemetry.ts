/**
 * Live telemetry state (WebSocket snapshot, session, status).
 * Stub: returns no active session until Block C wires real LiveTelemetryProvider.
 * When connected, snapshot will contain real-time data including tyresSurfaceTempC and fuelRemainingPercent.
 */
import type { WsSnapshotMessage } from "./types";
import type { Session } from "../api/types";

export type LiveStatus =
  | "live"
  | "waiting"
  | "no-data"
  | "disconnected"
  | "error";

export interface LiveTelemetryState {
  status: LiveStatus;
  session: Session | null;
  snapshot: WsSnapshotMessage | null;
}

const STUB_STATE: LiveTelemetryState = {
  status: "no-data",
  session: null,
  snapshot: null,
};

/**
 * Returns current live telemetry state (snapshot, session, status).
 * Replace with context consumer when LiveTelemetryProvider is implemented (Block C).
 */
export function useLiveTelemetry(): LiveTelemetryState {
  return STUB_STATE;
}
