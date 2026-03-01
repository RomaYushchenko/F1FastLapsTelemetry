/**
 * API base URL for REST. Set via VITE_API_BASE_URL in .env.
 * When unset: in browser uses '' (same-origin so /api goes through nginx proxy in Docker);
 * in Node/test uses http://localhost:8081 (backend default port).
 */
function getApiBaseUrl(): string {
  if (
    typeof import.meta.env.VITE_API_BASE_URL === 'string' &&
    import.meta.env.VITE_API_BASE_URL.length > 0
  ) {
    return import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
  }
  if (typeof window !== 'undefined' && window.location?.origin) {
    return ''
  }
  return 'http://localhost:8081'
}

export const API_BASE_URL = getApiBaseUrl()

/**
 * WebSocket URL (e.g. for live telemetry). Set via VITE_WS_URL in .env
 */
export const WS_URL =
  typeof import.meta.env.VITE_WS_URL === 'string' && import.meta.env.VITE_WS_URL.length > 0
    ? import.meta.env.VITE_WS_URL.replace(/\/$/, '')
    : (API_BASE_URL ? API_BASE_URL.replace(/^http/, 'ws') + '/ws' : '')

/**
 * SockJS endpoint for live telemetry. Base URL (HTTP) + /ws/live.
 * When API_BASE_URL is '' (same-origin), uses window.location.origin so requests go through nginx proxy.
 */
export function getWsLiveEndpoint(): string {
  let base: string
  if (
    typeof import.meta.env.VITE_WS_URL === 'string' &&
    import.meta.env.VITE_WS_URL.length > 0
  ) {
    base = import.meta.env.VITE_WS_URL.replace(/\/$/, '').replace(/^ws/, 'http')
  } else if (API_BASE_URL === '' && typeof window !== 'undefined' && window.location?.origin) {
    base = window.location.origin
  } else {
    base = API_BASE_URL || 'http://localhost:8081'
  }
  return `${base}/ws/live`
}
