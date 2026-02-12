import { Link, useLocation } from 'react-router-dom'

export function Header() {
  const location = useLocation()
  const isLive = location.pathname === '/'
  const isSessions = location.pathname === '/sessions' || location.pathname.startsWith('/sessions/')

  return (
    <header
      style={{
        height: '56px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: `0 var(--space-5)`,
        backgroundColor: 'var(--bg-elevated)',
        borderBottom: '1px solid var(--border)',
      }}
    >
      <Link
        to="/"
        style={{
          fontSize: 'var(--text-lg)',
          fontWeight: 'var(--font-weight-semibold)',
          color: 'var(--text-primary)',
          textDecoration: 'none',
        }}
      >
        F1 FastLaps Telemetry
      </Link>
      <nav style={{ display: 'flex', gap: 'var(--space-4)' }}>
        <Link
          to="/"
          style={{
            color: isLive ? 'var(--accent)' : 'var(--text-secondary)',
            fontWeight: isLive ? 'var(--font-weight-medium)' : undefined,
            textDecoration: 'none',
          }}
        >
          Live
        </Link>
        <Link
          to="/sessions"
          style={{
            color: isSessions ? 'var(--accent)' : 'var(--text-secondary)',
            fontWeight: isSessions ? 'var(--font-weight-medium)' : undefined,
            textDecoration: 'none',
          }}
        >
          Sessions
        </Link>
      </nav>
    </header>
  )
}
