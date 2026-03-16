import React from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { PacketHealthBand, SessionDiagnosticsDto } from '@/api/types'
import * as wsModule from '@/ws'
import * as clientModule from '@/api/client'
import * as notifyModule from '@/notify'
import Dashboard from './Dashboard'

describe('Dashboard packet health card', () => {
  beforeEach(() => {
    vi.spyOn(wsModule, 'useLiveTelemetry').mockReturnValue({
      status: 'live',
      session: {
        id: 'session-1',
        trackDisplayName: 'Silverstone',
        sessionType: 'Race',
      },
      errorMessage: null,
      positions: [],
    } as unknown as ReturnType<typeof wsModule.useLiveTelemetry>)

    vi.spyOn(clientModule, 'getSessions').mockResolvedValue({
      sessions: [],
      total: 0,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mockDiagnostics(band: PacketHealthBand, percent: number | null): void {
    const dto: SessionDiagnosticsDto = {
      sessionPublicId: 'session-1',
      packetLossRatio: percent != null ? 1 - percent / 100 : null,
      packetHealthBand: band,
      packetHealthPercent: percent,
    }
    vi.spyOn(clientModule, 'getSessionDiagnostics').mockResolvedValue(dto)
  }

  it('shows dash when diagnostics are unknown', async () => {
    mockDiagnostics('UNKNOWN', null)

    render(<Dashboard />)

    const label = await screen.findByText('Packet health (approx.)')
    expect(label).toBeInTheDocument()
    const value = screen.getByText('—')
    expect(value).toBeInTheDocument()
  })

  it('renders GOOD packet health percent', async () => {
    mockDiagnostics('GOOD', 95)

    render(<Dashboard />)

    const value = await screen.findByText('95%')
    expect(value).toBeInTheDocument()
  })

  it('applies correct indicator color for GOOD/OK/POOR', async () => {
    mockDiagnostics('GOOD', 95)
    const { rerender, container } = render(<Dashboard />)

    await screen.findByText('95%')
    let indicator = container.querySelector('span[aria-hidden="true"]')
    expect(indicator).not.toBeNull()
    expect(indicator).toHaveStyle({ backgroundColor: '#22C55E' })

    mockDiagnostics('OK', 80)
    rerender(<Dashboard />)
    await screen.findByText('80%')
    indicator = container.querySelector('span[aria-hidden="true"]')
    expect(indicator).not.toBeNull()
    expect(indicator).toHaveStyle({ backgroundColor: '#FACC15' })

    mockDiagnostics('POOR', 40)
    rerender(<Dashboard />)
    await screen.findByText('40%')
    indicator = container.querySelector('span[aria-hidden="true"]')
    expect(indicator).not.toBeNull()
    expect(indicator).toHaveStyle({ backgroundColor: '#EF4444' })
  })

  it('shows warning toast once when packet health is poor', async () => {
    const warningSpy = vi.spyOn(notifyModule.notify, 'warning').mockImplementation(() => {})

    mockDiagnostics('POOR', 40)
    render(<Dashboard />)

    await screen.findByText('40%')
    expect(warningSpy).toHaveBeenCalledTimes(1)

    mockDiagnostics('POOR', 30)
    render(<Dashboard />)
    await screen.findByText('30%')
    expect(warningSpy).toHaveBeenCalledTimes(1)
  })
}

