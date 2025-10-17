import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import SpaceListPage from '@/pages/space-list-page'
import SpaceDetailPage from '@/pages/space-detail-page'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<SpaceListPage />} />
        <Route path="/space/:spaceId" element={<SpaceDetailPage />} />
      </Routes>
    </Router>
  )
}

export default App

