import { Outlet, useLocation } from 'react-router-dom'
import { Link } from 'react-router-dom'
import { Header } from './Header'

export function AppLayout() {
  const location = useLocation()
  const isSessionDetail = /^\/sessions\/[^/]+$/.test(location.pathname)

  return (
    <>
      <Header />
      {isSessionDetail && (
        <div
          style={{
            padding: 'var(--space-2) var(--space-5)',
            backgroundColor: 'var(--bg-surface)',
            borderBottom: '1px solid var(--border)',
          }}
        >
          <Link
            to="/sessions"
            style={{
              color: 'var(--text-secondary)',
              fontSize: 'var(--text-sm)',
              textDecoration: 'none',
            }}
          >
            ← Back to Sessions
          </Link>
        </div>
      )}
      <main className="main-content">
        <Outlet />
      </main>
    </>
  )
}
