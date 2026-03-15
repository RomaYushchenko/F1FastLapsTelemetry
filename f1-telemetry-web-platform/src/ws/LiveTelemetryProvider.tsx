/**
 * Live telemetry context: single STOMP connection, getActiveSession polling,
 * auto-reconnect after disconnect/SESSION_ENDED. Exposes display status and snapshot.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import SockJS from 'sockjs-client'
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { getWsLiveEndpoint } from '@/api/config'
import { getActiveSession, getLeaderboard } from '@/api/client'
import { notify } from '@/notify'
import type { CarPositionDto, LeaderboardEntry, Session, SessionEventDto } from '@/api/types'
import type {
  WsErrorMessage,
  WsLeaderboardMessage,
  WsServerMessage,
  WsSessionEndedMessage,
  WsSessionEventMessage,
  WsSnapshotMessage,
} from './types'

const ACTIVE_SESSION_POLL_INTERVAL_MS = 4000
/** Refresh session from server while connected so trackId (from SessionData) is picked up for all tracks. */
const SESSION_REFRESH_WHILE_CONNECTED_MS = 5000

export type LiveStatus =
  | 'live'
  | 'waiting'
  | 'no-data'
  | 'disconnected'
  | 'error'

export interface LiveTelemetryState {
  status: LiveStatus
  session: Session | null
  snapshot: WsSnapshotMessage | null
  /** Live leaderboard (from WS or initial getLeaderboard). */
  leaderboard: LeaderboardEntry[]
  /** New session events pushed via WS (append to timeline). Block E optional 20.7. */
  sessionEventsAppend: SessionEventDto[]
  sessionEnded: WsSessionEndedMessage | null
  errorMessage: string | null
  /** Latest world positions for all cars (B9 POSITIONS messages). */
  positions: CarPositionDto[]
}

function toDisplayStatus(
  internal:
    | 'idle'
    | 'loading-active-session'
    | 'no-active-session'
    | 'connecting'
    | 'connected'
    | 'disconnected'
    | 'error'
): LiveStatus {
  switch (internal) {
    case 'connected':
      return 'live'
    case 'connecting':
    case 'loading-active-session':
    case 'idle':
      return 'waiting'
    case 'no-active-session':
      return 'no-data'
    case 'disconnected':
      return 'disconnected'
    case 'error':
      return 'error'
    default:
      return 'waiting'
  }
}

const defaultState: LiveTelemetryState = {
  status: 'no-data',
  session: null,
  snapshot: null,
  leaderboard: [],
  sessionEventsAppend: [],
  sessionEnded: null,
  errorMessage: null,
  positions: [],
}

const LiveTelemetryContext = createContext<LiveTelemetryState>(defaultState)

export function LiveTelemetryProvider({ children }: { children: ReactNode }) {
  const [internalStatus, setInternalStatus] = useState<
    'idle' | 'loading-active-session' | 'no-active-session' | 'connecting' | 'connected' | 'disconnected' | 'error'
  >('idle')
  const [session, setSession] = useState<Session | null>(null)
  const [snapshot, setSnapshot] = useState<WsSnapshotMessage | null>(null)
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([])
  const [sessionEventsAppend, setSessionEventsAppend] = useState<SessionEventDto[]>([])
  const [sessionEnded, setSessionEnded] = useState<WsSessionEndedMessage | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [positions, setPositions] = useState<CarPositionDto[]>([])

  const stompClientRef = useRef<Client | null>(null)
  const liveSubRef = useRef<StompSubscription | null>(null)
  const errorSubRef = useRef<StompSubscription | null>(null)
  const pollTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const hasInitiatedConnectRef = useRef(false)
  const isCancelledRef = useRef(false)

  const connectWithSession = useCallback((activeSession: Session) => {
    if (isCancelledRef.current) return
    const sessionId = activeSession.id

    setInternalStatus('connecting')
    setSession(activeSession)
    setSessionEventsAppend([])
    setErrorMessage(null)

    const client = new Client({
      webSocketFactory: () => new SockJS(getWsLiveEndpoint()),
      reconnectDelay: 0,
      debug: () => {},
    })
    stompClientRef.current = client

    client.onConnect = () => {
      if (isCancelledRef.current) {
        client.deactivate()
        return
      }

      liveSubRef.current = client.subscribe(
        `/topic/live/${sessionId}`,
        (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as WsServerMessage
            if (payload.type === 'SNAPSHOT') {
              setSnapshot(payload)
            } else if (payload.type === 'LEADERBOARD') {
              setLeaderboard((payload as WsLeaderboardMessage).entries ?? [])
            } else if (payload.type === 'SESSION_EVENT') {
              const ev = (payload as WsSessionEventMessage).event
              if (ev) setSessionEventsAppend((prev) => [...prev, ev])
            } else if (payload.type === 'POSITIONS') {
              setPositions(payload.positions ?? [])
            } else if (payload.type === 'SESSION_ENDED') {
              // Deactivate STOMP client so reconnect does not create a second connection
              const client = stompClientRef.current
              if (liveSubRef.current) {
                liveSubRef.current.unsubscribe()
                liveSubRef.current = null
              }
              if (errorSubRef.current) {
                errorSubRef.current.unsubscribe()
                errorSubRef.current = null
              }
              if (client?.connected) {
                try {
                  client.publish({
                    destination: '/app/unsubscribe',
                    body: JSON.stringify({ type: 'UNSUBSCRIBE' }),
                  })
                } catch {
                  // ignore
                }
              }
              if (client) {
                client.deactivate()
                stompClientRef.current = null
              }
              notify.info('Session ended')
              setSessionEnded(payload)
              setInternalStatus('disconnected')
              // Do not clear snapshot (keep last frame visible)
            }
          } catch (err) {
            const msg = err instanceof Error ? err.message : 'Failed to parse live message'
            notify.error(msg)
            setErrorMessage(msg)
            setInternalStatus('error')
          }
        }
      )

      errorSubRef.current = client.subscribe('/user/queue/errors', (message: IMessage) => {
        try {
          const payload = JSON.parse(message.body) as WsErrorMessage
          const msg = payload.message ?? payload.code ?? 'Connection error'
          notify.warning(msg)
          setErrorMessage(msg)
          setInternalStatus('error')
        } catch (err) {
          const msg = err instanceof Error ? err.message : 'Failed to parse error message'
          notify.warning(msg)
          setErrorMessage(msg)
          setInternalStatus('error')
        }
      })

      client.publish({
        destination: '/app/subscribe',
        body: JSON.stringify({
          type: 'SUBSCRIBE',
          sessionId,
          carIndex: activeSession.playerCarIndex ?? 0,
        }),
      })

      setInternalStatus('connected')
      notify.info('Live telemetry connected')

      // Initial leaderboard load; WS will push updates
      getLeaderboard()
        .then((entries) => {
          if (!isCancelledRef.current) setLeaderboard(entries)
        })
        .catch(() => { /* toast already from getLeaderboard */ })
    }

    client.onStompError = (frame) => {
      if (isCancelledRef.current) return
      const msg = frame.body || 'WebSocket protocol error'
      notify.error(msg)
      setErrorMessage(msg)
      setInternalStatus('error')
    }

    client.onWebSocketError = () => {
      if (isCancelledRef.current) return
      notify.error('WebSocket connection error')
      setErrorMessage('WebSocket connection error')
      setInternalStatus('error')
    }

    client.onDisconnect = () => {
      if (isCancelledRef.current) return
      notify.info('Disconnected from live feed')
      setInternalStatus('disconnected')
    }

    client.activate()
  }, [])

  const startOrPoll = useCallback(async () => {
    if (isCancelledRef.current || hasInitiatedConnectRef.current) return

    setInternalStatus('loading-active-session')
    setErrorMessage(null)

    let activeSession: Session | null = null
    try {
      activeSession = await getActiveSession()
    } catch (err) {
      if (isCancelledRef.current) return
      const msg = err instanceof Error ? err.message : 'Failed to load active session'
      notify.error(msg)
      setErrorMessage(msg)
      setInternalStatus('error')
      return
    }

    if (isCancelledRef.current) return

    if (!activeSession) {
      setInternalStatus('no-active-session')
      setSession(null)
      setLeaderboard([])
      setSessionEventsAppend([])
      setSessionEnded(null)
      setErrorMessage(null)
      setPositions([])
      // Schedule poll for auto-reconnect when session appears again
      pollTimeoutRef.current = setTimeout(() => {
        pollTimeoutRef.current = null
        startOrPoll()
      }, ACTIVE_SESSION_POLL_INTERVAL_MS)
      return
    }

    hasInitiatedConnectRef.current = true
    connectWithSession(activeSession)
  }, [connectWithSession])

  useEffect(() => {
    isCancelledRef.current = false
    hasInitiatedConnectRef.current = false
    startOrPoll()

    return () => {
      isCancelledRef.current = true
      if (pollTimeoutRef.current != null) {
        clearTimeout(pollTimeoutRef.current)
        pollTimeoutRef.current = null
      }
      const client = stompClientRef.current
      if (client?.connected) {
        try {
          client.publish({
            destination: '/app/unsubscribe',
            body: JSON.stringify({ type: 'UNSUBSCRIBE' }),
          })
        } catch {
          // ignore
        }
      }
      if (liveSubRef.current) {
        liveSubRef.current.unsubscribe()
        liveSubRef.current = null
      }
      if (errorSubRef.current) {
        errorSubRef.current.unsubscribe()
        errorSubRef.current = null
      }
      if (client) {
        client.deactivate()
        stompClientRef.current = null
      }
    }
  }, [startOrPoll])

  // While connected, periodically refresh session so trackId (set by SessionData after SSTA) is available for Live Track Map on all tracks
  useEffect(() => {
    if (internalStatus !== 'connected' || session?.id == null) return
    const sessionId = session.id
    const interval = setInterval(async () => {
      try {
        const fresh = await getActiveSession()
        if (fresh?.id === sessionId) setSession(fresh)
      } catch {
        // ignore; keep current session
      }
    }, SESSION_REFRESH_WHILE_CONNECTED_MS)
    return () => clearInterval(interval)
  }, [internalStatus, session?.id])

  // Auto-reconnect: when status is disconnected, re-poll getActiveSession
  useEffect(() => {
    if (internalStatus !== 'disconnected') return
    hasInitiatedConnectRef.current = false
    const t = setTimeout(() => {
      startOrPoll()
    }, ACTIVE_SESSION_POLL_INTERVAL_MS)
    return () => clearTimeout(t)
  }, [internalStatus, startOrPoll])

  const state = useMemo<LiveTelemetryState>(
    () => ({
      status: toDisplayStatus(internalStatus),
      session,
      snapshot,
      leaderboard,
      sessionEventsAppend,
      sessionEnded,
      errorMessage,
      positions,
    }),
    [internalStatus, session, snapshot, leaderboard, sessionEventsAppend, sessionEnded, errorMessage, positions]
  )

  return (
    <LiveTelemetryContext.Provider value={state}>
      {children}
    </LiveTelemetryContext.Provider>
  )
}

export function useLiveTelemetry(): LiveTelemetryState {
  return useContext(LiveTelemetryContext)
}
