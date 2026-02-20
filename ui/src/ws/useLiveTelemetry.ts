import { useEffect, useMemo, useState } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { WS_URL } from '../api/config'
import { getActiveSession } from '../api/client'
import type { Session } from '../api/types'
import type { WsErrorMessage, WsServerMessage, WsSessionEndedMessage, WsSnapshotMessage } from './types'

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

      // WebSocket is only opened when there is an active session; otherwise server shows 0 connections
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
        return
      }

      if (isCancelled) return

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

        // Subscribe to live snapshots (topic uses session UUID)
        liveSubscription = stompClient.subscribe(
          `/topic/live/${sessionId}`,
          (message: IMessage) => {
            try {
              const payload = JSON.parse(message.body) as WsServerMessage
              if (payload.type === 'SNAPSHOT') {
                setState(prev => ({
                  ...prev,
                  snapshot: payload,
                }))
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

        // Subscribe to error queue
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

        // Send subscribe command (session id = UUID)
        stompClient.publish({
          destination: '/app/subscribe',
          body: JSON.stringify({
            type: 'SUBSCRIBE',
            sessionId,
            carIndex: 0,
          }),
        })

        setState(prev => ({
          ...prev,
          status: 'connected',
        }))
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

    start()

    return () => {
      isCancelled = true

      try {
        // Send unsubscribe if connected
        if (stompClient.connected) {
          stompClient.publish({
            destination: '/app/unsubscribe',
            body: JSON.stringify({ type: 'UNSUBSCRIBE' }),
          })
        }
      } catch {
        // ignore
      }

      if (liveSubscription) {
        liveSubscription.unsubscribe()
      }
      if (errorSubscription) {
        errorSubscription.unsubscribe()
      }

      stompClient.deactivate()
    }
  }, [stompClient])

  return state
}

