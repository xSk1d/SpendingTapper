import { Navigate, Route, Routes } from 'react-router'
import QuickAddPage from './routes/QuickAddPage'
import HistoryPage from './routes/HistoryPage'
import SettingsPage from './routes/SettingsPage'

export default function App() {
  return (
    <div className="app">
      <Routes>
        {/* Launching the app IS the entry screen. There is no home screen to get
            past, so a back-tap lands straight on the keypad. */}
        <Route path="/" element={<QuickAddPage />} />
        <Route path="/edit/:id" element={<QuickAddPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
