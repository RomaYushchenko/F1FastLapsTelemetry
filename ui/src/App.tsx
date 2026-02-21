import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AppLayout } from './components/AppLayout'
import { LiveDashboardPage } from './pages/LiveDashboardPage'
import { SessionListPage } from './pages/SessionListPage'
import { SessionDetailPage } from './pages/SessionDetailPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route index element={<LiveDashboardPage />} />
          <Route path="sessions" element={<SessionListPage />} />
          <Route path="sessions/:sessionUid" element={<SessionDetailPage />} />
        </Route>
      </Routes>
      <Toaster
        position="bottom-right"
        richColors={false}
        offset={24}
        toastOptions={{
          style: {
            background: 'var(--bg-surface)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--text-primary)',
            boxShadow: 'var(--shadow-md)',
          },
          classNames: {
            toast: 'toast-app',
          },
        }}
      />
    </BrowserRouter>
  )
}

export default App
