/**
 * API base URL for REST. Default: http://localhost:8081 (telemetry-processing-api-service)
 * Set via VITE_API_BASE_URL in .env
 */
export const API_BASE_URL =
  typeof import.meta.env.VITE_API_BASE_URL === 'string' &&
  import.meta.env.VITE_API_BASE_URL.length > 0
    ? import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
    : 'http://localhost:8081'

/**
 * WebSocket URL for SockJS (e.g. for live telemetry).
 * Default: same as API_BASE_URL. Set via VITE_WS_URL in .env
 */
export const WS_URL =
  typeof import.meta.env.VITE_WS_URL === 'string' && import.meta.env.VITE_WS_URL.length > 0
    ? import.meta.env.VITE_WS_URL.replace(/\/$/, '')
    : API_BASE_URL
