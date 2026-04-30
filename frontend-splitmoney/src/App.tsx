import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import { GoogleOAuthHandler } from './components/auth/GoogleOAuthHandler'
import { ErrorBoundary } from './components/ui/ErrorBoundary'
import ProtectedRoute from './components/auth/ProtectedRoute'

import Landing    from './pages/Landing'
import SignIn     from './pages/auth/SignIn'
import SignUp     from './pages/auth/SignUp'
import Forgot     from './pages/auth/Forgot'
import Dashboard  from './pages/Dashboard'
import Groups     from './pages/Groups'
import GroupDetail from './pages/GroupDetail'
import NewGroup   from './pages/NewGroup'
import AddExpense from './pages/AddExpense'
import SettleUp   from './pages/SettleUp'
import Friends    from './pages/Friends'
import Activity   from './pages/Activity'
import Analytics  from './pages/Analytics'
import Profile    from './pages/Profile'
import Settings      from './pages/Settings'
import PaymentReturn from './pages/PaymentReturn'

function AppRoutes() {
  return (
    <>
      <GoogleOAuthHandler />
      <Routes>
        <Route path="/"        element={<Landing />} />
        <Route path="/signin"  element={<SignIn />} />
        <Route path="/signup"  element={<SignUp />} />
        <Route path="/forgot"  element={<Forgot />} />

        <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/groups"    element={<ProtectedRoute><Groups /></ProtectedRoute>} />
        <Route path="/groups/new" element={<ProtectedRoute><NewGroup /></ProtectedRoute>} />
        <Route path="/groups/:id" element={<ProtectedRoute><GroupDetail /></ProtectedRoute>} />
        <Route path="/add"       element={<ProtectedRoute><AddExpense /></ProtectedRoute>} />
        <Route path="/settle"    element={<ProtectedRoute><SettleUp /></ProtectedRoute>} />
        <Route path="/friends"   element={<ProtectedRoute><Friends /></ProtectedRoute>} />
        <Route path="/activity"  element={<ProtectedRoute><Activity /></ProtectedRoute>} />
        <Route path="/analytics" element={<ProtectedRoute><Analytics /></ProtectedRoute>} />
        <Route path="/profile"   element={<ProtectedRoute><Profile /></ProtectedRoute>} />
        <Route path="/settings"  element={<ProtectedRoute><Settings /></ProtectedRoute>} />
        <Route path="/payment/return" element={<ProtectedRoute><PaymentReturn /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <NotificationProvider>
            <AppRoutes />
          </NotificationProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
