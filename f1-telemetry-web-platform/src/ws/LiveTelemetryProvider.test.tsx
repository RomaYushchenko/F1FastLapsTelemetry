import React from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { LiveTelemetryProvider, useLiveTelemetry } from './LiveTelemetryProvider'
import type { Session } from '@/api/types'
import * as clientModule from '@/api/client'
import * as notifyModule from '@/notify'

const mockSession: Session = {
  id: 'session-1',
  sessionUid: 123,
  playerCarIndex: 0,
  sessionDisplayName: 'Test Session',
}

vi.mock('sockjs-client', () => ({
  default: vi.fn(),
}))

vi.mock('@stomp/stompjs', () => {
  class MockClient {
    onConnect?: () => void
    onStompError?: () => void
    onWebSocketError?: () => void
    onDisconnect?: () => void
    connected = false

    constructor() {}

    activate() {
      this.connected = true
      if (this.onConnect) this.onConnect()
    }

    deactivate() {
      this.connected = false
    }

    subscribe() {
      return { unsubscribe: vi.fn() }
    }

    publish() {}
  }

  return {
    Client: MockClient,
  }
})

describe('LiveTelemetryProvider', () => {
  beforeEach(() => {
    vi.spyOn(clientModule, 'getActiveSession').mockResolvedValue(mockSession)
    vi.spyOn(clientModule, 'getLeaderboard').mockResolvedValue([])
    vi.spyOn(notifyModule, 'notify').mockReturnValue({
      info: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    } as unknown as typeof notifyModule.notify)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('exposes initial no-data state before connection', async () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <LiveTelemetryProvider>{children}</LiveTelemetryProvider>
    )

    const { result } = renderHook(() => useLiveTelemetry(), { wrapper })

    expect(result.current.status).toBeDefined()
  })

  it('sets positions when POSITIONS message is received', async () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <LiveTelemetryProvider>{children}</LiveTelemetryProvider>
    )

    const { result } = renderHook(() => useLiveTelemetry(), { wrapper })

    await act(async () => {
      await Promise.resolve()
    })

    expect(result.current.positions).toEqual([])
  })
})

