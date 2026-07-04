import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import Vote from './pages/Vote'
import Manage from './pages/Manage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/votes/:shareCode" element={<Vote />} />
        <Route path="/votes/:shareCode/manage" element={<Manage />} />
      </Routes>
    </BrowserRouter>
  )
}
