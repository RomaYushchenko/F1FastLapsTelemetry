/**
 * REST API client. Uses notify for non-404 errors so toasts and Bell list stay in sync.
 */

import { notify } from '@/notify'
import { API_BASE_URL } from './config'
import { HttpError } from './types'
import type {
  ApiErrorBody,
  ComparisonResponseDto,
  ErsByLapDto,
  ErsPoint,
  FuelByLapDto,
  Lap,
  LapCorner,
  LeaderboardEntry,
  PacePoint,
  PedalTracePoint,
  PitStopDto,
  Session,
  SessionEventDto,
  SessionSummary,
  SpeedTracePoint,
  StintDto,
  TrackLayoutResponseDto,
  TrackLayoutStatusDto,
  TrackLayoutExportDto,
  TrackLayoutBulkExportDto,
  TyreWearPoint,
} from './types'

async function parseJsonOrNull(response: Response): Promise<unknown | null> {
  const contentType = response.headers.get('content-type')
  if (!contentType || !contentType.includes('application/json')) return null
  if (response.status === 204) return null
  return response.json()
}

interface RequestOptions extends RequestInit {
  signal?: AbortSignal
}

async function requestJson<T>(path: string, init?: RequestOptions): Promise<T> {
  const url = `${API_BASE_URL}${path}`

  const response = await fetch(url, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (response.ok) {
    return (await parseJsonOrNull(response)) as T
  }

  const body = await parseJsonOrNull(response)
  const message =
    (body &&
    typeof body === 'object' &&
    'message' in body &&
    typeof (body as ApiErrorBody).message === 'string'
      ? (body as ApiErrorBody).message
      : `Request to ${path} failed with status ${response.status}`) ?? ''

  if (response.status !== 404) {
    notify.error(message)
  }
  throw new HttpError(response.status, message, body ?? undefined)
}

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}

/** Response from GET /api/sessions with list and total count from X-Total-Count header. */
export interface GetSessionsResult {
  sessions: Session[]
  total: number
}

function carIndexQuery(carIndex: number): string {
  return carIndex !== 0 ? `?carIndex=${carIndex}` : ''
}

export interface GetSessionsParams {
  limit?: number
  offset?: number
  sessionType?: string
  trackId?: number
  search?: string
  sort?: string
  state?: string
  dateFrom?: string
  dateTo?: string
}

export async function getSessions(params?: GetSessionsParams): Promise<GetSessionsResult> {
  const limit = params?.limit ?? 50
  const offset = params?.offset ?? 0
  const query: Record<string, string | number | undefined> = {
    limit,
    offset,
    sessionType: params?.sessionType,
    trackId: params?.trackId,
    search: params?.search,
    sort: params?.sort ?? 'startedAt_desc',
    state: params?.state,
    dateFrom: params?.dateFrom,
    dateTo: params?.dateTo,
  }
  const url = `${API_BASE_URL}/api/sessions${buildQuery(query)}`
  const response = await fetch(url, { headers: { Accept: 'application/json' } })

  if (!response.ok) {
    const body = await parseJsonOrNull(response)
    const message =
      (body &&
        typeof body === 'object' &&
        'message' in body &&
        typeof (body as ApiErrorBody).message === 'string'
        ? (body as ApiErrorBody).message
        : `Request to /api/sessions failed with status ${response.status}`) ?? ''
    if (response.status !== 404) {
      notify.error(message)
    }
    throw new HttpError(response.status, message, body ?? undefined)
  }

  const sessions = (await response.json()) as Session[]
  const totalHeader = response.headers.get('X-Total-Count')
  const total = totalHeader != null ? parseInt(totalHeader, 10) : sessions.length
  const totalCount = Number.isNaN(total) ? sessions.length : total
  return { sessions, total: totalCount }
}

export async function getSession(sessionUid: string | undefined): Promise<Session> {
  return requestJson<Session>(`/api/sessions/${encodeURIComponent(sessionUid!)}`)
}

/** GET /api/sessions/{sessionUid}/comparison — Driver Comparison (optional referenceLapNumA/B). */
export async function getComparison(
  sessionUid: string | undefined,
  carIndexA: number,
  carIndexB: number,
  referenceLapNumA?: number | null,
  referenceLapNumB?: number | null
): Promise<ComparisonResponseDto> {
  const params: Record<string, string | number> = {
    carIndexA,
    carIndexB,
  }
  if (referenceLapNumA != null) params.referenceLapNumA = referenceLapNumA
  if (referenceLapNumB != null) params.referenceLapNumB = referenceLapNumB
  return requestJson<ComparisonResponseDto>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/comparison${buildQuery(params)}`
  )
}

export async function getSessionLaps(
  sessionUid: string | undefined,
  carIndex = 0
): Promise<Lap[]> {
  return requestJson<Lap[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/laps${carIndexQuery(carIndex)}`
  )
}

export async function getSessionSummary(
  sessionUid: string | undefined,
  carIndex = 0
): Promise<SessionSummary> {
  return requestJson<SessionSummary>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/summary${carIndexQuery(carIndex)}`
  )
}

export async function getActiveSession(
  options?: RequestOptions
): Promise<Session | null> {
  const url = `${API_BASE_URL}/api/sessions/active`
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options?.headers ?? {}),
    },
  })

  if (response.status === 204) return null
  if (response.ok) {
    return (await response.json()) as Session
  }

  const body = await parseJsonOrNull(response)
  const message =
    (body &&
    typeof body === 'object' &&
    'message' in body &&
    typeof (body as ApiErrorBody).message === 'string'
      ? (body as ApiErrorBody).message
      : `Request to /api/sessions/active failed with status ${response.status}`) ?? ''

  if (response.status !== 404) {
    notify.error(message)
  }
  throw new HttpError(response.status, message, body ?? undefined)
}

export async function updateSessionDisplayName(
  sessionId: string,
  sessionDisplayName: string
): Promise<Session> {
  return requestJson<Session>(
    `/api/sessions/${encodeURIComponent(sessionId)}`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionDisplayName }),
    }
  )
}

// Chart endpoints

export async function getSessionPace(
  sessionUid: string | undefined,
  carIndex = 0
): Promise<PacePoint[]> {
  return requestJson<PacePoint[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/pace${carIndexQuery(carIndex)}`
  )
}

export async function getLapTrace(
  sessionUid: string | undefined,
  lapNum: number,
  carIndex = 0
): Promise<PedalTracePoint[]> {
  return requestJson<PedalTracePoint[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/laps/${lapNum}/trace${carIndexQuery(carIndex)}`
  )
}

export async function getLapErs(
  sessionUid: string | undefined,
  lapNum: number,
  carIndex = 0
): Promise<ErsPoint[]> {
  return requestJson<ErsPoint[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/laps/${lapNum}/ers${carIndexQuery(carIndex)}`
  )
}

export async function getLapSpeedTrace(
  sessionUid: string | undefined,
  lapNum: number,
  carIndex = 0
): Promise<SpeedTracePoint[]> {
  return requestJson<SpeedTracePoint[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/laps/${lapNum}/speed-trace${carIndexQuery(carIndex)}`
  )
}

export async function getLapCorners(
  sessionUid: string | undefined,
  lapNum: number,
  carIndex = 0
): Promise<LapCorner[]> {
  return requestJson<LapCorner[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/laps/${lapNum}/corners${carIndexQuery(carIndex)}`
  )
}

export async function getSessionTyreWear(
  sessionUid: string | undefined,
  carIndex = 0
): Promise<TyreWearPoint[]> {
  return requestJson<TyreWearPoint[]>(
    `/api/sessions/${encodeURIComponent(sessionUid!)}/tyre-wear${carIndexQuery(carIndex)}`
  )
}

export async function getPitStops(
  sessionId: string | undefined,
  carIndex = 0
): Promise<PitStopDto[]> {
  return requestJson<PitStopDto[]>(
    `/api/sessions/${encodeURIComponent(sessionId!)}/pit-stops${carIndexQuery(carIndex)}`
  )
}

export async function getStints(
  sessionId: string | undefined,
  carIndex = 0
): Promise<StintDto[]> {
  return requestJson<StintDto[]>(
    `/api/sessions/${encodeURIComponent(sessionId!)}/stints${carIndexQuery(carIndex)}`
  )
}

/** GET /api/sessions/{id}/fuel-by-lap — fuel (kg) at lap end (B6). */
export async function getFuelByLap(
  sessionId: string | undefined,
  carIndex = 0
): Promise<FuelByLapDto[]> {
  return requestJson<FuelByLapDto[]>(
    `/api/sessions/${encodeURIComponent(sessionId!)}/fuel-by-lap${carIndexQuery(carIndex)}`
  )
}

/** GET /api/sessions/{id}/ers-by-lap — ERS store % at lap end (B7). */
export async function getErsByLap(
  sessionId: string | undefined,
  carIndex = 0
): Promise<ErsByLapDto[]> {
  return requestJson<ErsByLapDto[]>(
    `/api/sessions/${encodeURIComponent(sessionId!)}/ers-by-lap${carIndexQuery(carIndex)}`
  )
}

/** GET /api/sessions/active/positions — live positions for all cars (B9); 204 → empty list */
export async function getActivePositions(options?: RequestOptions): Promise<import('./types').CarPositionDto[]> {
  const url = `${API_BASE_URL}/api/sessions/active/positions`
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options?.headers ?? {}),
    },
  })
  if (response.status === 204) return []
  if (!response.ok) {
    const body = await parseJsonOrNull(response)
    const message =
      (body &&
      typeof body === 'object' &&
      'message' in body &&
      typeof (body as ApiErrorBody).message === 'string'
        ? (body as ApiErrorBody).message
        : `Request to /api/sessions/active/positions failed with status ${response.status}`) ?? ''
    notify.error(message)
    throw new HttpError(response.status, message, body ?? undefined)
  }
  return response.json() as Promise<import('./types').CarPositionDto[]>
}

/** GET /api/sessions/active/leaderboard — live leaderboard; 204 → empty list */
export async function getLeaderboard(options?: RequestOptions): Promise<LeaderboardEntry[]> {
  const url = `${API_BASE_URL}/api/sessions/active/leaderboard`
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options?.headers ?? {}),
    },
  })
  if (response.status === 204) return []
  if (!response.ok) {
    const body = await parseJsonOrNull(response)
    const message =
      (body &&
      typeof body === 'object' &&
      'message' in body &&
      typeof (body as ApiErrorBody).message === 'string'
        ? (body as ApiErrorBody).message
        : `Request to /api/sessions/active/leaderboard failed with status ${response.status}`) ?? ''
    notify.error(message)
    throw new HttpError(response.status, message, body ?? undefined)
  }
  return response.json() as Promise<LeaderboardEntry[]>
}

/** GET /api/sessions/{id}/events — session events for timeline */
export async function getSessionEvents(
  sessionId: string,
  params?: { fromLap?: number; toLap?: number; limit?: number }
): Promise<SessionEventDto[]> {
  const query: Record<string, string | number | undefined> = {}
  if (params?.fromLap != null) query.fromLap = params.fromLap
  if (params?.toLap != null) query.toLap = params.toLap
  if (params?.limit != null) query.limit = params.limit
  const q = Object.keys(query).length ? buildQuery(query) : ''
  return requestJson<SessionEventDto[]>(
    `/api/sessions/${encodeURIComponent(sessionId)}/events${q}`
  )
}

/** GET /api/sessions/{id}/export?format=csv|json — download session data as file. Block I — Step 31. */
export async function exportSession(
  sessionId: string,
  format: 'json' | 'csv'
): Promise<void> {
  const url = `${API_BASE_URL}/api/sessions/${encodeURIComponent(sessionId)}/export?format=${format}`
  const response = await fetch(url, { headers: { Accept: format === 'json' ? 'application/json' : 'text/csv' } })
  if (!response.ok) {
    const body = await parseJsonOrNull(response)
    const message =
      (body &&
        typeof body === 'object' &&
        'message' in body &&
        typeof (body as ApiErrorBody).message === 'string'
        ? (body as ApiErrorBody).message
        : `Export failed with status ${response.status}`) ?? ''
    notify.error(message)
    throw new HttpError(response.status, message, body ?? undefined)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition')
  const suggestedName =
    disposition?.match(/filename="?([^";\n]+)"?/)?.[1] ||
    `session-${sessionId}-export.${format}`
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = suggestedName
  link.click()
  URL.revokeObjectURL(link.href)
  notify.success('Export downloaded')
}

/** GET /api/tracks/{trackId}/layout — 2D track map; 404 → null (layout not available). */
export async function getTrackLayout(
  trackId: number,
  options?: RequestOptions
): Promise<TrackLayoutResponseDto | null> {
  const url = `${API_BASE_URL}/api/tracks/${trackId}/layout`
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options?.headers ?? {}),
    },
  })
  if (response.status === 404) return null
  if (response.ok) {
    return (await response.json()) as TrackLayoutResponseDto
  }
  const body = await parseJsonOrNull(response)
  const message =
    (body &&
    typeof body === 'object' &&
    'message' in body &&
    typeof (body as ApiErrorBody).message === 'string'
      ? (body as ApiErrorBody).message
      : `Request to /api/tracks/${trackId}/layout failed with status ${response.status}`) ?? ''
  notify.error(message)
  throw new HttpError(response.status, message, body ?? undefined)
}

/** GET /api/tracks/{trackId}/layout/status — layout recording/availability status. */
export async function getTrackLayoutStatus(trackId: number): Promise<TrackLayoutStatusDto> {
  const path = `/api/tracks/${trackId}/layout/status`
  return requestJson<TrackLayoutStatusDto>(path)
}

/** GET /api/tracks/{trackId}/layout/export — download single track layout JSON. */
export async function exportTrackLayout(trackId: number): Promise<void> {
  const url = `${API_BASE_URL}/api/tracks/${trackId}/layout/export`
  const response = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!response.ok) {
    const body = await parseJsonOrNull(response)
    const message =
      (body &&
        typeof body === 'object' &&
        'message' in body &&
        typeof (body as ApiErrorBody).message === 'string'
        ? (body as ApiErrorBody).message
        : `Export track layout failed with status ${response.status}`) ?? ''
    notify.error(message)
    throw new HttpError(response.status, message, body ?? undefined)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition')
  const suggestedName =
    disposition?.match(/filename="?([^";\n]+)"?/)?.[1] ||
    `track-${trackId}-layout.json`
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = suggestedName
  link.click()
  URL.revokeObjectURL(link.href)
  notify.success('Track layout exported')
}

/** GET /api/tracks/layout/export-all — download all track layouts JSON. */
export async function exportAllTrackLayouts(): Promise<void> {
  const url = `${API_BASE_URL}/api/tracks/layout/export-all`
  const response = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!response.ok) {
    const body = await parseJsonOrNull(response)
    const message =
      (body &&
        typeof body === 'object' &&
        'message' in body &&
        typeof (body as ApiErrorBody).message === 'string'
        ? (body as ApiErrorBody).message
        : `Export all track layouts failed with status ${response.status}`) ?? ''
    notify.error(message)
    throw new HttpError(response.status, message, body ?? undefined)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition')
  const suggestedName =
    disposition?.match(/filename="?([^";\n]+)"?/)?.[1] ||
    'all-tracks-layout.json'
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = suggestedName
  link.click()
  URL.revokeObjectURL(link.href)
  notify.success('All track layouts exported')
}

/** POST /api/tracks/layout/import — import single track layout from JSON. */
export async function importTrackLayout(dto: TrackLayoutExportDto): Promise<TrackLayoutResponseDto> {
  const path = '/api/tracks/layout/import'
  return requestJson<TrackLayoutResponseDto>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  })
}

/** POST /api/tracks/layout/import-all — import multiple track layouts from JSON. */
export async function importAllTrackLayouts(dto: TrackLayoutBulkExportDto): Promise<{
  imported: number
  skipped: number
  errors: string[]
}> {
  const path = '/api/tracks/layout/import-all'
  return requestJson<{
    imported: number
    skipped: number
    errors: string[]
  }>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dto),
  })
}
