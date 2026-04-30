import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthShell } from '../../components/layout/AuthShell'
import Button from '../../components/ui/Button'
import Field from '../../components/ui/Field'
import Input from '../../components/ui/Input'
import OtpModal from '../../components/auth/OtpModal'
import { useAuth } from '../../context/AuthContext'
import { registerUser } from '../../api/auth'
import { extractMessage } from '../../utils/messages'

const USE_OPTIONS = [
  { k: 'roommates', emoji: '🏠', label: 'Roommates' },
  { k: 'trip',      emoji: '✈️', label: 'A trip' },
  { k: 'couple',    emoji: '💑', label: 'Couple' },
  { k: 'other',     emoji: '👯', label: 'Something else' },
]

export default function SignUp() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const [step, setStep] = useState(1)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [mobile, setMobile] = useState('')
  const [pw, setPw] = useState('')
  const [confirmPw, setConfirmPw] = useState('')
  const [use, setUse] = useState('roommates')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // OTP modal
  const [otpOpen, setOtpOpen] = useState(false)
  const [otpEmail, setOtpEmail] = useState('')
  const [otpMobile, setOtpMobile] = useState('')

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true })
  }, [isAuthenticated, navigate])

  // ── Step 1 submit ─────────────────────────────────────────────────────────
  const handleStep1 = (e) => {
    e.preventDefault()
    if (pw !== confirmPw) { setError('Passwords do not match.'); return }
    if (pw.length < 12)   { setError('Password must be at least 12 characters.'); return }
    setError('')
    setStep(2)
  }

  // ── Step 2 (final) submit ─────────────────────────────────────────────────
  const handleStep2 = async () => {
    setError('')
    setLoading(true)
    try {
      const result = await registerUser({ email: email.trim(), mobile: mobile.trim() || null, password: pw })

      // Already registered but unverified
      if (!result.ok && result.body?.responseCode === 'VERIFICATION_PENDING') {
        setOtpEmail(result.body?.data?.email ?? email.trim())
        setOtpMobile(result.body?.data?.mobile ?? mobile.trim())
        setOtpOpen(true)
        return
      }

      if (!result.ok || !result.body?.success) {
        setError(extractMessage(result.body, 'Unable to register. Please try again.'))
        setStep(1)
        return
      }

      // Registered — open OTP verification
      setOtpEmail(email.trim())
      setOtpMobile(mobile.trim())
      setOtpOpen(true)
    } catch {
      setError('Network error. Please try again.')
      setStep(1)
    } finally {
      setLoading(false)
    }
  }

  const handleOtpSuccess = () => {
    setOtpOpen(false)
    navigate('/signin', { replace: true, state: { success: 'Account verified! Please sign in.' } })
  }

  return (
    <AuthShell>
      {/* Progress bar */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 28 }}>
        {[1, 2].map(n => (
          <div key={n} style={{
            flex: 1, height: 4, borderRadius: 2,
            background: step >= n ? 'var(--sm-green-500)' : 'var(--bg4)',
            transition: 'background 300ms',
          }} />
        ))}
      </div>

      <div style={{ marginBottom: 28 }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 40, letterSpacing: '-0.03em', margin: 0, lineHeight: 1.05 }}>
          {step === 1 ? 'Create your account.' : 'What will you split?'}
        </h1>
        <p style={{ color: 'var(--fg2)', fontSize: 15, marginTop: 8 }}>
          {step === 1
            ? <> Already have one? <Link to="/signin" style={{ color: 'var(--sm-green-600)', fontWeight: 600, textDecoration: 'none' }}>Sign in →</Link></>
            : "We'll personalise your experience."}
        </p>
      </div>

      {error && (
        <div style={{ background: '#FFE5E5', color: '#C22', borderRadius: 12, padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16 }}>
          {error}
        </div>
      )}

      {/* ── Step 1 ──────────────────────────────────────────────────────── */}
      {step === 1 && (
        <form onSubmit={handleStep1} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Field label="Full name">
            <Input value={name} onChange={e => setName(e.target.value)} placeholder="Riley Chen" required />
          </Field>
          <Field label="Email">
            <Input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@email.com" required />
          </Field>
          <Field label="Mobile (optional)" hint="International format, e.g. +919876543210. Used for SMS OTP.">
            <Input type="tel" value={mobile} onChange={e => setMobile(e.target.value)} placeholder="+919876543210" />
          </Field>
          <Field label="Password" hint="At least 12 characters.">
            <Input type="password" value={pw} onChange={e => setPw(e.target.value)} placeholder="••••••••••••" required />
          </Field>
          <Field label="Confirm password">
            <Input type="password" value={confirmPw} onChange={e => setConfirmPw(e.target.value)} placeholder="••••••••••••" required />
          </Field>
          <Button type="submit" variant="dark" size="lg" style={{ width: '100%', marginTop: 8 }}>
            Continue →
          </Button>
        </form>
      )}

      {/* ── Step 2 ──────────────────────────────────────────────────────── */}
      {step === 2 && (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 24 }}>
            {USE_OPTIONS.map(o => (
              <button
                key={o.k}
                onClick={() => setUse(o.k)}
                style={{
                  padding: 20, borderRadius: 16, cursor: 'pointer', textAlign: 'left',
                  border: use === o.k ? '2px solid var(--sm-green-500)' : '1.5px solid var(--border-default)',
                  background: use === o.k ? 'var(--sm-green-50)' : 'white',
                  transition: 'all 150ms',
                }}
              >
                <div style={{ fontSize: 28 }}>{o.emoji}</div>
                <div style={{ fontWeight: 600, marginTop: 8, fontSize: 15, color: 'var(--fg1)' }}>{o.label}</div>
              </button>
            ))}
          </div>
          <Button
            variant="dark"
            size="lg"
            style={{ width: '100%' }}
            disabled={loading}
            onClick={handleStep2}
          >
            {loading ? 'Creating account…' : 'Get started →'}
          </Button>
          <button
            onClick={() => { setStep(1); setError('') }}
            style={{ background: 'none', border: 'none', color: 'var(--fg3)', cursor: 'pointer', marginTop: 12, fontSize: 13, width: '100%' }}
          >
            ← Back
          </button>
        </>
      )}

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
