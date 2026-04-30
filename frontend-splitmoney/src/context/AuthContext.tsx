import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCurrentSession, logoutUser } from '../api/auth'
import { registerUnauthorizedHandler, clearUnauthorizedHandler } from '../api/http'
import type { ApiBody, AuthContextValue, Session } from '../types'

const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

// Maps raw backend 401 messages to user-friendly copy.
function friendlySessionMessage(responseMessage: string | null | undefined): string {
  const msg = (responseMessage ?? '').toLowerCase()
  if (msg.includes('missing') || msg.includes('expired') || msg.includes('not found')) {
    return 'Your session has expired. Please sign in again.'
  }
  if (msg.includes('invalid') || msg.includes('malformed')) {
    return 'Your session is no longer valid. Please sign in again.'
  }
  if (msg.includes('refresh token')) {
    return 'Authentication error. Please sign in again.'
  }
  return 'Your session has expired. Please sign in again.'
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()

  const [session, setSession] = useState<Session | null>(null)
  const [isBootstrappingSession, setIsBootstrappingSession] = useState(true)
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null)

  // ── Session bootstrap ─────────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false
    const restore = async () => {
      try {
        const result = await getCurrentSession()
        if (cancelled) return
        if (result.ok && result.body?.success && result.body?.data) {
          const d = result.body.data
          setSession({
            email: d.email ?? '',
            userId: d.userId ?? null,
            roles: Array.isArray(d.roles) ? d.roles : [],
            accessTokenExpiresAt: d.accessTokenExpiresAt ?? null,
          })
        }
      } catch {
        // silent failure — user sees sign-in screen
      } finally {
        if (!cancelled) setIsBootstrappingSession(false)
      }
    }
    restore()
    return () => { cancelled = true }
  }, [])

  // ── Token countdown ───────────────────────────────────────────────────────
  useEffect(() => {
    if (!session?.accessTokenExpiresAt) { setRemainingSeconds(null); return }
    const update = () => {
      const ms = new Date(session.accessTokenExpiresAt!).getTime()
      setRemainingSeconds(Math.max(0, Math.floor((ms - Date.now()) / 1000)))
    }
    update()
    const id = window.setInterval(update, 1000)
    return () => window.clearInterval(id)
  }, [session?.accessTokenExpiresAt])

  const handleUnauthorized = useCallback((responseBody: ApiBody | null) => {
    setSession(null)
    navigate('/signin', {
      replace: true,
      state: {
        sessionExpired: true,
        message: friendlySessionMessage((responseBody as any)?.responseMessage),
      },
    })
  }, [navigate])

  // ── Register global 401 interceptor ──────────────────────────────────────
  useEffect(() => {
    registerUnauthorizedHandler((body) => handleUnauthorized(body as ApiBody | null))
    return () => clearUnauthorizedHandler()
  }, [handleUnauthorized])

  const applySession = useCallback((data: Partial<Session>) => {
    setSession({
      email: data?.email ?? '',
      userId: data?.userId ?? null,
      roles: Array.isArray(data?.roles) ? data.roles! : [],
      accessTokenExpiresAt: data?.accessTokenExpiresAt ?? null,
    })
  }, [])

  const logout = useCallback(async () => {
    try { await logoutUser() } catch { /* clear local session regardless */ }
    setSession(null)
    navigate('/signin', { replace: true })
  }, [navigate])

  return (
    <AuthContext.Provider value={{
      session, setSession, applySession,
      isBootstrappingSession, remainingSeconds,
      handleUnauthorized, logout,
      isAuthenticated: Boolean(session),
    }}>
      {children}
    </AuthContext.Provider>
  )
}
