/**
 * API base URL for REST. Set via VITE_API_BASE_URL in .env
 */
export const API_BASE_URL =
  typeof import.meta.env.VITE_API_BASE_URL === 'string' &&
  import.meta.env.VITE_API_BASE_URL.length > 0
    ? import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
    : 'http://localhost:8080'

/**
 * WebSocket URL (e.g. for live telemetry). Set via VITE_WS_URL in .env
 */
export const WS_URL =
  typeof import.meta.env.VITE_WS_URL === 'string' && import.meta.env.VITE_WS_URL.length > 0
    ? import.meta.env.VITE_WS_URL.replace(/\/$/, '')
    : (API_BASE_URL.replace(/^http/, 'ws') + '/ws')
