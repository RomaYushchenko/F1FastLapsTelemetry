/**
 * Session id in the API: same value for list, get, laps, summary, and WebSocket.
 * We always use string IDs (toSessionIdString) so numeric ids from JSON keep precision.
 */

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

/** Normalize session id to string (handles API accidentally sending a number). */
export function toSessionIdString(id: unknown): string {
  if (id == null) return ''
  if (typeof id === 'string') return id
  if (typeof id === 'number' && Number.isFinite(id)) return String(id)
  return String(id)
}

/** Return true if the value looks like a session UUID. */
export function isSessionUuid(id: string): boolean {
  return id.length > 0 && UUID_REGEX.test(id)
}

const NUMERIC_ID_REGEX = /^\d+$/

/** Return true if the value is a valid session id: UUID or numeric (session_uid). */
export function isValidSessionId(id: string): boolean {
  if (id == null || id.length === 0) return false
  const s = id.trim()
  return UUID_REGEX.test(s) || NUMERIC_ID_REGEX.test(s)
}
