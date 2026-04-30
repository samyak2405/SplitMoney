import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthShell } from '../../components/layout/AuthShell'
import Button from '../../components/ui/Button'
import Field from '../../components/ui/Field'
import Input from '../../components/ui/Input'
import OtpModal from '../../components/auth/OtpModal'
import { useAuth } from '../../context/AuthContext'
import { loginUser } from '../../api/auth'
import { createGoogleAuthorizationUrl } from '../../utils/oauthGoogle'
import { extractMessage } from '../../utils/messages'

const GOOGLE_CLIENT_ID  = import.meta.env.VITE_GOOGLE_CLIENT_ID || import.meta.env.GOOGLE_OAUTH_CLIENT_ID || ''
const GOOGLE_REDIRECT_URI = import.meta.env.VITE_GOOGLE_REDIRECT_URI || window.location.origin

// ── Social buttons ────────────────────────────────────────────────────────────
function SocialButtons({ onGoogle, loading }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
      <SocialBtn label="Google" loading={loading} onClick={onGoogle} svg={
        <svg width="18" height="18" viewBox="0 0 24 24">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.76h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.76c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0012 23z"/>
          <path fill="#FBBC05" d="M5.84 14.11A6.6 6.6 0 015.5 12c0-.73.13-1.44.34-2.11V7.05H2.18A11 11 0 001 12c0 1.77.43 3.45 1.18 4.95l3.66-2.84z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.46 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.05l3.66 2.84C6.71 7.31 9.14 5.38 12 5.38z"/>
        </svg>
      }/>
      <SocialBtn label="Apple" svg={
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M17.05 12.04c-.03-2.97 2.42-4.39 2.53-4.46-1.38-2.02-3.53-2.3-4.3-2.33-1.83-.19-3.57 1.08-4.5 1.08-.93 0-2.37-1.05-3.89-1.02-2 .03-3.84 1.16-4.87 2.95-2.08 3.6-.53 8.93 1.5 11.85.99 1.43 2.17 3.04 3.72 2.98 1.5-.06 2.06-.96 3.87-.96 1.8 0 2.31.96 3.89.93 1.6-.03 2.62-1.45 3.6-2.89 1.13-1.66 1.6-3.27 1.62-3.35-.04-.02-3.1-1.19-3.14-4.72zM14.08 3.4c.82-.99 1.38-2.37 1.22-3.73-1.18.05-2.6.78-3.45 1.77-.76.87-1.43 2.26-1.25 3.6 1.31.1 2.66-.66 3.48-1.64z"/>
        </svg>
      }/>
      <SocialBtn label="GitHub" svg={
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 .3a12 12 0 00-3.79 23.4c.6.1.82-.26.82-.58v-2c-3.34.73-4.04-1.6-4.04-1.6-.55-1.4-1.34-1.77-1.34-1.77-1.09-.74.08-.73.08-.73 1.2.08 1.84 1.24 1.84 1.24 1.07 1.83 2.82 1.3 3.5.99.11-.78.42-1.3.76-1.6-2.66-.3-5.47-1.34-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.3-.54-1.52.12-3.18 0 0 1.01-.32 3.3 1.23a11.5 11.5 0 016 0c2.29-1.55 3.3-1.23 3.3-1.23.66 1.66.24 2.88.12 3.18.77.84 1.24 1.9 1.24 3.22 0 4.6-2.8 5.63-5.48 5.92.43.38.82 1.11.82 2.24v3.33c0 .32.22.7.83.58A12 12 0 0012 .3"/>
        </svg>
      }/>
    </div>
  )
}

function SocialBtn({ label, svg, onClick, loading }) {
  return (
    <button
      onClick={onClick}
      disabled={loading}
      style={{
        padding: '11px 0', borderRadius: 12, border: '1.5px solid var(--border-default)',
        background: 'white', cursor: loading ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        gap: 8, fontSize: 14, fontWeight: 600, color: 'var(--fg1)',
        opacity: loading ? 0.6 : 1,
      }}
    >
      {svg} {label}
    </button>
  )
}

function Divider() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '20px 0', color: 'var(--fg3)', fontSize: 12 }}>
      <div style={{ flex: 1, height: 1, background: 'var(--border-default)' }} />
      or with email
      <div style={{ flex: 1, height: 1, background: 'var(--border-default)' }} />
    </div>
  )
}

// ── main component ────────────────────────────────────────────────────────────
export default function SignIn() {
  const navigate = useNavigate()
  const location = useLocation()
  const { applySession, isAuthenticated } = useAuth()

  const [email,    setEmail]    = useState('')
  const [pw,       setPw]       = useState('')
  const [error,    setError]    = useState(location.state?.error || '')
  const [success,  setSuccess]  = useState('')
  const sessionExpired = Boolean(location.state?.sessionExpired)
  const sessionMessage = location.state?.message as string | undefined
  const [loading,  setLoading]  = useState(false)
  const [googleLoading, setGoogleLoading] = useState(false)

  // OTP modal state
  const [otpOpen,  setOtpOpen]  = useState(false)
  const [otpEmail, setOtpEmail] = useState('')
  const [otpMobile, setOtpMobile] = useState('')

  // If already authenticated, go straight to dashboard
  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true })
  }, [isAuthenticated, navigate])

  // ── login ────────────────────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault()
    const trimmedEmail = email.trim()
    if (!trimmedEmail || !pw) { setError('Please enter your email and password.'); return }
    setError(''); setSuccess('')
    setLoading(true)
    try {
      const result = await loginUser({ email: trimmedEmail, password: pw })

      // Account not verified → open OTP modal
      if (result.status === 403 && result.body?.data?.code === 'ACCOUNT_NOT_VERIFIED') {
        setSuccess('Please verify your account first.')
        setOtpEmail(result.body.data?.email ?? trimmedEmail)
        setOtpMobile(result.body.data?.mobile ?? '')
        setOtpOpen(true)
        return
      }

      if (!result.ok || !result.body?.success) {
        setError(extractMessage(result.body, 'Unable to log in. Check your credentials.'))
        return
      }

      applySession(result.body.data)
      navigate('/dashboard', { replace: true })
    } catch {
      setError('Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  // ── Google OAuth ─────────────────────────────────────────────────────────
  const handleGoogleSignIn = async () => {
    if (!GOOGLE_CLIENT_ID) {
      setError('Google OAuth is not configured. Set VITE_GOOGLE_CLIENT_ID.')
      return
    }
    setError(''); setSuccess('')
    setGoogleLoading(true)
    try {
      const url = await createGoogleAuthorizationUrl({ clientId: GOOGLE_CLIENT_ID, redirectUri: GOOGLE_REDIRECT_URI })
      window.location.assign(url)
    } catch (err) {
      setError(err?.message || 'Unable to start Google login.')
      setGoogleLoading(false)
    }
  }

  // ── OTP success ──────────────────────────────────────────────────────────
  const handleOtpSuccess = () => {
    setOtpOpen(false)
    setSuccess('Account verified! Please sign in.')
  }

  return (
    <AuthShell>
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 44, letterSpacing: '-0.03em', margin: 0, lineHeight: 1.05 }}>
          Welcome back.
        </h1>
        <p style={{ color: 'var(--fg2)', fontSize: 15, marginTop: 8 }}>
          No account?{' '}
          <Link to="/signup" style={{ color: 'var(--sm-green-600)', fontWeight: 600, textDecoration: 'none' }}>
            Sign up free →
          </Link>
        </p>
      </div>

      {sessionExpired && (
        <div style={{
          display: 'flex', alignItems: 'flex-start', gap: 10,
          background: '#FFF8EC', color: '#92400E',
          border: '1px solid #FCD34D', borderRadius: 12,
          padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16,
        }}>
          <span style={{ fontSize: 16, lineHeight: 1.4 }}>🔒</span>
          <span>{sessionMessage ?? 'Your session has expired. Please sign in again.'}</span>
        </div>
      )}
      {error && !sessionExpired && (
        <div style={{ background: '#FFE5E5', color: '#C22', borderRadius: 12, padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16 }}>
          {error}
        </div>
      )}
      {success && !error && !sessionExpired && (
        <div style={{ background: 'var(--sm-green-50)', color: 'var(--sm-green-700)', borderRadius: 12, padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16 }}>
          {success}
        </div>
      )}

      <SocialButtons onGoogle={handleGoogleSignIn} loading={googleLoading} />
      <Divider />

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <Field label="Email">
          <Input
            type="email" value={email} onChange={e => setEmail(e.target.value)}
            placeholder="you@email.com" autoComplete="email" required
          />
        </Field>
        <Field label={
          <span style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
            <span>Password</span>
            <Link to="/forgot" style={{ color: 'var(--sm-green-600)', fontWeight: 600, fontSize: 12, textDecoration: 'none' }}>
              Forgot?
            </Link>
          </span>
        }>
          <Input
            type="password" value={pw} onChange={e => setPw(e.target.value)}
            placeholder="••••••••" autoComplete="current-password" required
          />
        </Field>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--fg2)', cursor: 'pointer' }}>
          <input type="checkbox" defaultChecked style={{ accentColor: 'var(--sm-green-500)' }} />
          Keep me signed in
        </label>
        <Button type="submit" variant="dark" size="lg" disabled={loading} style={{ width: '100%', marginTop: 8 }}>
          {loading ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>

      <p style={{ textAlign: 'center', fontSize: 12, color: 'var(--fg3)', marginTop: 24 }}>
        By signing in you agree to our{' '}
        <a href="#" style={{ color: 'var(--fg2)' }}>Terms</a> and{' '}
        <a href="#" style={{ color: 'var(--fg2)' }}>Privacy Policy</a>.
      </p>

      <OtpModal
        open={otpOpen}
        email={otpEmail}
        mobile={otpMobile}
        onSuccess={handleOtpSuccess}
        onClose={() => setOtpOpen(false)}
      />
    </AuthShell>
  )
}
