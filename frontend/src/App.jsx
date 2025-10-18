import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import SpaceListPage from '@/pages/space-list-page'
import SpaceDetailPage from '@/pages/space-detail-page'
import LogsMonitorPage from '@/pages/logs-monitor-page'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<SpaceListPage />} />
        <Route path="/space/:spaceId" element={<SpaceDetailPage />} />
        <Route path="/logs" element={<LogsMonitorPage />} />
      </Routes>
    </Router>
  )
}

export default App

