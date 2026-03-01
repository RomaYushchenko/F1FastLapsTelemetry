/**
 * Format helpers for lap/sector times (ms → display string).
 */

/**
 * Format lap time in ms as M:SS.mmm (e.g. 1:27.451).
 */
export function formatLapTime(ms: number | null | undefined): string {
  if (ms == null || ms < 0) return '—'
  const totalSec = ms / 1000
  const minutes = Math.floor(totalSec / 60)
  const seconds = totalSec - minutes * 60
  return `${minutes}:${seconds.toFixed(3).padStart(6, '0')}`
}

/**
 * Format sector time in ms as SS.mmm.
 */
export function formatSector(ms: number | null | undefined): string {
  if (ms == null || ms < 0) return '—'
  const sec = ms / 1000
  return sec.toFixed(3)
}

/**
 * Format session time in seconds (from game) as M:SS.mmm (e.g. 2:05.500).
 */
export function formatSessionTime(seconds: number | null | undefined): string {
  if (seconds == null || seconds < 0) return '—'
  const minutes = Math.floor(seconds / 60)
  const sec = seconds - minutes * 60
  return `${minutes}:${sec.toFixed(3).padStart(6, '0')}`
}
