import { BrowserRouter, Routes, Route } from 'react-router-dom'
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
    </BrowserRouter>
  )
}

export default App
