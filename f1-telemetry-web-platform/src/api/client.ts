/**
 * REST API client. Uses notify for non-404 errors so toasts and Bell list stay in sync.
 */

import { notify } from '@/notify'
import { API_BASE_URL } from './config'
import { HttpError } from './types'
import type {
  ApiErrorBody,
  ErsPoint,
  Lap,
  LapCorner,
  PacePoint,
  PedalTracePoint,
  Session,
  SessionSummary,
  SpeedTracePoint,
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

function buildQuery(params: Record<string, number | undefined>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) search.set(key, String(value))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}

function carIndexQuery(carIndex: number): string {
  return carIndex !== 0 ? `?carIndex=${carIndex}` : ''
}

export async function getSessions(params?: {
  limit?: number
  offset?: number
}): Promise<Session[]> {
  const limit = params?.limit ?? 20
  const offset = params?.offset ?? 0
  return requestJson<Session[]>(
    `/api/sessions${buildQuery({ limit, offset })}`
  )
}

export async function getSession(sessionUid: string | undefined): Promise<Session> {
  return requestJson<Session>(`/api/sessions/${encodeURIComponent(sessionUid!)}`)
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
