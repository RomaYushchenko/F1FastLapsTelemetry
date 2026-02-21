import { API_BASE_URL } from './config'
import { HttpError } from './types'
import type { ApiErrorBody, Lap, Session, SessionSummary } from './types'
import type { PacePoint, PedalTracePoint, TyreWearPoint } from '../charts/types'

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
      'Accept': 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (response.ok) {
    return (await parseJsonOrNull(response)) as T
  }

  const body = await parseJsonOrNull(response)
  const message =
    (body && typeof body === 'object' && 'message' in body && typeof (body as ApiErrorBody).message === 'string'
      ? (body as ApiErrorBody).message
      : `Request to ${path} failed with status ${response.status}`) ?? ''

  throw new HttpError(response.status, message, body ?? undefined)
}

export async function getSessions(): Promise<Session[]> {
  return requestJson<Session[]>('/api/sessions')
}

export async function getSession(sessionUid: string | undefined): Promise<Session> {
  return requestJson<Session>(`/api/sessions/${sessionUid}`)
}

export async function updateSessionDisplayName(
  sessionId: string,
  sessionDisplayName: string,
): Promise<Session> {
  return requestJson<Session>(`/api/sessions/${encodeURIComponent(sessionId)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionDisplayName }),
  })
}

export async function getSessionLaps(
  sessionUid: string | undefined,
  carIndex = 0,
): Promise<Lap[]> {
  const search = new URLSearchParams()
  if (carIndex !== 0) search.set('carIndex', String(carIndex))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return requestJson<Lap[]>(`/api/sessions/${sessionUid}/laps${suffix}`)
}

export async function getSessionSummary(
  sessionUid: string | undefined,
  carIndex = 0,
): Promise<SessionSummary> {
  const search = new URLSearchParams()
  if (carIndex !== 0) search.set('carIndex', String(carIndex))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return requestJson<SessionSummary>(`/api/sessions/${sessionUid}/summary${suffix}`)
}

export async function getActiveSession(options?: RequestOptions): Promise<Session | null> {
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
    (body && typeof body === 'object' && 'message' in body && typeof (body as ApiErrorBody).message === 'string'
      ? (body as ApiErrorBody).message
      : `Request to /api/sessions/active failed with status ${response.status}`) ?? ''

  throw new HttpError(response.status, message, body ?? undefined)
}

export async function getSessionPace(
  sessionUid: string | undefined,
  carIndex = 0,
): Promise<PacePoint[]> {
  const search = new URLSearchParams()
  if (carIndex !== 0) search.set('carIndex', String(carIndex))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return requestJson<PacePoint[]>(`/api/sessions/${sessionUid}/pace${suffix}`)
}

export async function getLapTrace(
  sessionUid: string | undefined,
  lapNum: number,
  carIndex = 0,
): Promise<PedalTracePoint[]> {
  const search = new URLSearchParams()
  if (carIndex !== 0) search.set('carIndex', String(carIndex))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return requestJson<PedalTracePoint[]>(`/api/sessions/${sessionUid}/laps/${lapNum}/trace${suffix}`)
}

export async function getSessionTyreWear(
  sessionUid: string | undefined,
  carIndex = 0,
): Promise<TyreWearPoint[]> {
  const search = new URLSearchParams()
  if (carIndex !== 0) search.set('carIndex', String(carIndex))
  const suffix = search.toString() ? `?${search.toString()}` : ''
  return requestJson<TyreWearPoint[]>(`/api/sessions/${sessionUid}/tyre-wear${suffix}`)
}


