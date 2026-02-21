import { useEffect, useMemo, useState } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { WS_URL } from '../api/config'
import { getActiveSession } from '../api/client'
import type { Session } from '../api/types'
import type { WsErrorMessage, WsServerMessage, WsSessionEndedMessage, WsSnapshotMessage } from './types'

const ACTIVE_SESSION_POLL_INTERVAL_MS = 4000

export type LiveConnectionStatus =
  | 'idle'
  | 'loading-active-session'
  | 'no-active-session'
  | 'connecting'
  | 'connected'
  | 'disconnected'
  | 'error'

export interface LiveTelemetryState {
  status: LiveConnectionStatus
  session: Session | null
  snapshot: WsSnapshotMessage | null
  sessionEnded: WsSessionEndedMessage | null
  errorMessage: string | null
  connectionMessage: string | null
}

export function useLiveTelemetry() {
  const [state, setState] = useState<LiveTelemetryState>({
    status: 'idle',
    session: null,
    snapshot: null,
    sessionEnded: null,
    errorMessage: null,
    connectionMessage: null,
  })

  const stompClient = useMemo(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_URL}/ws/live`),
      reconnectDelay: 0,
      debug: () => {},
    })
    return client
  }, [])

  useEffect(() => {
    let isCancelled = false
    let liveSubscription: StompSubscription | null = null
    let errorSubscription: StompSubscription | null = null
    let pollTimeoutId: ReturnType<typeof setTimeout> | null = null
    let hasInitiatedConnect = false

    function connectWithSession(activeSession: Session) {
      setState(prev => ({
        ...prev,
        status: 'connecting',
        session: activeSession,
        errorMessage: null,
        connectionMessage: null,
      }))

      const sessionId = activeSession.id

      stompClient.onConnect = () => {
        if (isCancelled) {
          stompClient.deactivate()
          return
        }

        liveSubscription = stompClient.subscribe(
          `/topic/live/${sessionId}`,
          (message: IMessage) => {
            try {
              const payload = JSON.parse(message.body) as WsServerMessage
              if (payload.type === 'SNAPSHOT') {
                setState(prev => ({ ...prev, snapshot: payload }))
              } else if (payload.type === 'SESSION_ENDED') {
                setState(prev => ({
                  ...prev,
                  status: 'disconnected',
                  sessionEnded: payload,
                }))
              }
            } catch (error) {
              setState(prev => ({
                ...prev,
                status: 'error',
                errorMessage: error instanceof Error ? error.message : 'Failed to parse live message',
              }))
            }
          },
        )

        errorSubscription = stompClient.subscribe('/user/queue/errors', (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as WsErrorMessage
            setState(prev => ({
              ...prev,
              status: 'error',
              errorMessage: payload.message,
              connectionMessage: 'Connection lost. Live data may be outdated.',
            }))
          } catch (error) {
            setState(prev => ({
              ...prev,
              status: 'error',
              errorMessage: error instanceof Error ? error.message : 'Failed to parse error message',
              connectionMessage: 'Connection lost. Live data may be outdated.',
            }))
          }
        })

        stompClient.publish({
          destination: '/app/subscribe',
          body: JSON.stringify({
            type: 'SUBSCRIBE',
            sessionId,
            carIndex: activeSession.playerCarIndex ?? 0,
          }),
        })

        setState(prev => ({ ...prev, status: 'connected' }))
      }

      stompClient.onStompError = frame => {
        if (isCancelled) return
        setState(prev => ({
          ...prev,
          status: 'error',
          errorMessage: frame.body || 'WebSocket protocol error',
          connectionMessage: 'Connection lost. Live data may be outdated.',
        }))
      }

      stompClient.onWebSocketError = () => {
        if (isCancelled) return
        setState(prev => ({
          ...prev,
          status: 'error',
          errorMessage: 'WebSocket connection error',
          connectionMessage: 'Connection lost. Live data may be outdated.',
        }))
      }

      stompClient.onDisconnect = () => {
        if (isCancelled) return
        setState(prev => ({
          ...prev,
          status: 'disconnected',
          connectionMessage: 'Disconnected from live feed.',
        }))
      }

      stompClient.activate()
    }

    async function start() {
      setState(prev => ({
        ...prev,
        status: 'loading-active-session',
        errorMessage: null,
        connectionMessage: null,
      }))

      let activeSession: Session | null = null
      try {
        activeSession = await getActiveSession()
      } catch (error) {
        if (isCancelled) return
        setState(prev => ({
          ...prev,
          status: 'error',
          errorMessage: error instanceof Error ? error.message : 'Failed to load active session',
          connectionMessage: 'Connection lost. Live data may be outdated.',
        }))
        return
      }

      if (!activeSession) {
        if (isCancelled) return
        setState(prev => ({
          ...prev,
          status: 'no-active-session',
          session: null,
          snapshot: null,
          sessionEnded: null,
          connectionMessage: null,
        }))
        // Serialized polling: schedule next only after current poll completes (avoids duplicate connect when request is slower than interval)
        function schedulePoll() {
          if (isCancelled) return
          pollTimeoutId = setTimeout(async () => {
            pollTimeoutId = null
            if (isCancelled || hasInitiatedConnect) return
            try {
              const session = await getActiveSession()
              if (isCancelled || hasInitiatedConnect) return
              if (session) {
                hasInitiatedConnect = true
                connectWithSession(session)
                return
              }
            } catch {
              // ignore poll errors; next poll will retry
            }
            schedulePoll()
          }, ACTIVE_SESSION_POLL_INTERVAL_MS)
        }
        schedulePoll()
        return
      }

      if (isCancelled) return
      connectWithSession(activeSession)
    }

    start()

    return () => {
      isCancelled = true
      if (pollTimeoutId != null) {
        clearTimeout(pollTimeoutId)
        pollTimeoutId = null
      }
      try {
        if (stompClient.connected) {
          stompClient.publish({
            destination: '/app/unsubscribe',
            body: JSON.stringify({ type: 'UNSUBSCRIBE' }),
          })
        }
      } catch {
        // ignore
      }
      if (liveSubscription) liveSubscription.unsubscribe()
      if (errorSubscription) errorSubscription.unsubscribe()
      stompClient.deactivate()
    }
  }, [stompClient])

  return state
}

