import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthShell } from '../../components/layout/AuthShell'
import Button from '../../components/ui/Button'
import Field from '../../components/ui/Field'
import Input from '../../components/ui/Input'
import { forgotPassword, resetPassword } from '../../api/auth'
import { extractMessage } from '../../utils/messages'
import { getRequestMeta } from '../../utils/requestMeta'

type Step = 'identify' | 'otp' | 'newPassword' | 'done'

export default function Forgot() {
  const navigate = useNavigate()

  // ── State ─────────────────────────────────────────────────────────────────
  const [step, setStep]         = useState<Step>('identify')
  const [identifier, setIdentifier] = useState('')
  const [identifierType, setIdentifierType] = useState<'email' | 'mobile'>('email')
  const [channel, setChannel]   = useState('')     // EMAIL | SMS from backend
  const [otp, setOtp]           = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm]   = useState('')
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)

  // Refs for OTP digit inputs
  const otpRefs = useRef<(HTMLInputElement | null)[]>([])

  // ── Step 1: Send OTP ──────────────────────────────────────────────────────
  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = identifier.trim()
    if (!trimmed) { setError('Please enter your email or mobile number.'); return }
    setError('')
    setLoading(true)
    try {
      const payload = identifierType === 'email'
        ? { ...getRequestMeta(), email: trimmed }
        : { ...getRequestMeta(), mobile: trimmed }
      const result = await forgotPassword(payload)
      if (!result.ok || !result.body?.success) {
        setError(extractMessage(result.body, 'Unable to send OTP. Please try again.'))
        return
      }
      setChannel(result.body.data?.deliveryChannel ?? identifierType.toUpperCase())
      setStep('otp')
    } catch {
      setError('Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  // ── Step 2: Verify OTP (UI only — actual verification in step 3) ──────────
  const handleOtpContinue = (e: React.FormEvent) => {
    e.preventDefault()
    if (otp.length !== 6 || !/^\d{6}$/.test(otp)) {
      setError('Please enter the 6-digit OTP.')
      return
    }
    setError('')
    setStep('newPassword')
  }

  // Handle OTP digit input: auto-advance focus
  const handleOtpDigit = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return
    const digits = otp.split('')
    digits[index] = value.slice(-1)
    const newOtp = digits.join('').slice(0, 6).padEnd(6, '').trimEnd()
    setOtp(digits.slice(0, 6).join(''))
    if (value && index < 5) otpRefs.current[index + 1]?.focus()
  }

  const handleOtpKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpRefs.current[index - 1]?.focus()
    }
  }

  // ── Step 3: Reset password ────────────────────────────────────────────────
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (password.length < 8) { setError('Password must be at least 8 characters.'); return }
    if (password !== confirm) { setError('Passwords do not match.'); return }
    setError('')
    setLoading(true)
    try {
      const payload = identifierType === 'email'
        ? { ...getRequestMeta(), email: identifier.trim(), otp, newPassword: password, confirmPassword: confirm }
        : { ...getRequestMeta(), mobile: identifier.trim(), otp, newPassword: password, confirmPassword: confirm }
      const result = await resetPassword(payload)
      if (!result.ok || !result.body?.success) {
        const msg = extractMessage(result.body, 'Unable to reset password.')
        // If OTP error, send back to OTP step
        if (msg.toLowerCase().includes('otp') || msg.toLowerCase().includes('expired')) {
          setOtp('')
          setStep('otp')
        }
        setError(msg)
        return
      }
      setStep('done')
    } catch {
      setError('Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  // ── Resend OTP ────────────────────────────────────────────────────────────
  const handleResend = async () => {
    setError('')
    setLoading(true)
    try {
      const payload = identifierType === 'email'
        ? { ...getRequestMeta(), email: identifier.trim() }
        : { ...getRequestMeta(), mobile: identifier.trim() }
      const result = await forgotPassword(payload)
      if (!result.ok || !result.body?.success) {
        setError(extractMessage(result.body, 'Unable to resend OTP.'))
        return
      }
      setOtp('')
      otpRefs.current[0]?.focus()
    } catch {
      setError('Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  // ── Step indicator ────────────────────────────────────────────────────────
  const steps = ['Identify', 'Verify OTP', 'New Password']
  const stepIndex = step === 'identify' ? 0 : step === 'otp' ? 1 : 2

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <AuthShell>
      <div style={{ marginBottom: 24 }}>
        <Link to="/signin" style={{ color: 'var(--fg3)', fontSize: 13, textDecoration: 'none' }}>
          ← Back to sign in
        </Link>
        <h1 style={{
          fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 40,
          letterSpacing: '-0.03em', margin: '12px 0 4px', lineHeight: 1.05,
        }}>
          {step === 'done' ? 'Password reset!' : 'Reset your password.'}
        </h1>
        <p style={{ color: 'var(--fg2)', fontSize: 15, margin: 0 }}>
          {step === 'identify'    && "Enter the email or mobile linked to your account."}
          {step === 'otp'         && `We sent a 6-digit OTP to your ${channel || identifierType}.`}
          {step === 'newPassword' && 'Choose a strong new password.'}
          {step === 'done'        && 'Your password has been updated. Sign in with your new password.'}
        </p>
      </div>

      {/* Step dots (not shown on done screen) */}
      {step !== 'done' && (
        <div style={{ display: 'flex', gap: 6, marginBottom: 24 }}>
          {steps.map((label, i) => (
            <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 6, flex: i < steps.length - 1 ? 1 : 0 }}>
              <div style={{
                width: 28, height: 28, borderRadius: 999, flexShrink: 0,
                background: i < stepIndex ? 'var(--sm-green-500)' : i === stepIndex ? 'var(--sm-ink-950)' : 'var(--bg4)',
                color: i <= stepIndex ? 'white' : 'var(--fg3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 13, fontWeight: 700,
              }}>
                {i < stepIndex ? '✓' : i + 1}
              </div>
              <span style={{ fontSize: 12, fontWeight: 600, color: i === stepIndex ? 'var(--fg1)' : 'var(--fg3)', whiteSpace: 'nowrap' }}>
                {label}
              </span>
              {i < steps.length - 1 && (
                <div style={{ flex: 1, height: 2, background: i < stepIndex ? 'var(--sm-green-500)' : 'var(--bg4)', minWidth: 16 }} />
              )}
            </div>
          ))}
        </div>
      )}

      {error && (
        <div style={{
          background: '#FFE5E5', color: '#C22', borderRadius: 12,
          padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16,
        }}>
          {error}
        </div>
      )}

      {/* ── Step 1: Identify ───────────────────────────────────────────────── */}
      {step === 'identify' && (
        <form onSubmit={handleSendOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Toggle: email vs mobile */}
          <div style={{ display: 'flex', gap: 6, padding: 4, background: 'var(--bg3)', borderRadius: 12 }}>
            {(['email', 'mobile'] as const).map(t => (
              <button
                key={t} type="button"
                onClick={() => { setIdentifierType(t); setIdentifier('') }}
                style={{
                  flex: 1, padding: '8px', border: 'none', borderRadius: 8, cursor: 'pointer',
                  background: identifierType === t ? 'white' : 'transparent',
                  boxShadow: identifierType === t ? 'var(--shadow-xs)' : 'none',
                  fontSize: 13, fontWeight: 600,
                  color: identifierType === t ? 'var(--fg1)' : 'var(--fg3)',
                }}
              >
                {t === 'email' ? 'Email' : 'Mobile'}
              </button>
            ))}
          </div>

          <Field label={identifierType === 'email' ? 'Email address' : 'Mobile number'}>
            <Input
              type={identifierType === 'email' ? 'email' : 'tel'}
              value={identifier}
              onChange={e => setIdentifier(e.target.value)}
              placeholder={identifierType === 'email' ? 'you@email.com' : '+91 9876543210'}
              required
            />
          </Field>

          <Button type="submit" variant="dark" size="lg" disabled={loading} style={{ width: '100%', marginTop: 8 }}>
            {loading ? 'Sending OTP…' : 'Send OTP →'}
          </Button>
        </form>
      )}

      {/* ── Step 2: OTP ────────────────────────────────────────────────────── */}
      {step === 'otp' && (
        <form onSubmit={handleOtpContinue} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <Field label={`Enter the 6-digit OTP sent to ${identifier}`}>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
              {Array.from({ length: 6 }).map((_, i) => (
                <input
                  key={i}
                  ref={el => { otpRefs.current[i] = el }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={otp[i] ?? ''}
                  onChange={e => handleOtpDigit(i, e.target.value)}
                  onKeyDown={e => handleOtpKeyDown(i, e)}
                  style={{
                    width: 48, height: 56, textAlign: 'center',
                    fontSize: 24, fontWeight: 700, fontVariantNumeric: 'tabular-nums',
                    border: '2px solid var(--border-default)', borderRadius: 12,
                    outline: 'none', background: 'white', color: 'var(--fg1)',
                    transition: 'border-color 180ms',
                  }}
                  onFocus={e => { e.target.style.borderColor = 'var(--sm-green-500)' }}
                  onBlur={e  => { e.target.style.borderColor = 'var(--border-default)' }}
                />
              ))}
            </div>
          </Field>

          <div style={{ display: 'flex', gap: 10 }}>
            <Button variant="ghost" size="lg" type="button" onClick={() => { setStep('identify'); setOtp('') }} style={{ flex: 1 }}>
              ← Back
            </Button>
            <Button variant="dark" size="lg" type="submit" style={{ flex: 2 }}>
              Verify OTP →
            </Button>
          </div>

          <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--fg3)', margin: 0 }}>
            Didn't receive it?{' '}
            <button
              type="button"
              onClick={handleResend}
              disabled={loading}
              style={{ background: 'none', border: 'none', color: 'var(--sm-green-600)', fontWeight: 600, cursor: 'pointer', fontSize: 13 }}
            >
              {loading ? 'Sending…' : 'Resend OTP'}
            </button>
          </p>
        </form>
      )}

      {/* ── Step 3: New password ────────────────────────────────────────────── */}
      {step === 'newPassword' && (
        <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Field label="New password">
            <Input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Min 8 characters"
              required
            />
          </Field>
          <Field label="Confirm new password">
            <Input
              type="password"
              value={confirm}
              onChange={e => setConfirm(e.target.value)}
              placeholder="Repeat your new password"
              required
            />
          </Field>

          {/* Password strength hint */}
          {password.length > 0 && password.length < 8 && (
            <p style={{ margin: 0, fontSize: 12, color: 'var(--sm-danger)' }}>
              Password is too short (minimum 8 characters)
            </p>
          )}
          {password.length >= 8 && password !== confirm && confirm.length > 0 && (
            <p style={{ margin: 0, fontSize: 12, color: 'var(--sm-danger)' }}>Passwords do not match</p>
          )}
          {password.length >= 8 && password === confirm && confirm.length > 0 && (
            <p style={{ margin: 0, fontSize: 12, color: 'var(--sm-green-600)' }}>✓ Passwords match</p>
          )}

          <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
            <Button variant="ghost" size="lg" type="button" onClick={() => setStep('otp')} style={{ flex: 1 }}>
              ← Back
            </Button>
            <Button variant="dark" size="lg" type="submit" disabled={loading} style={{ flex: 2 }}>
              {loading ? 'Resetting…' : 'Reset Password'}
            </Button>
          </div>
        </form>
      )}

      {/* ── Done ───────────────────────────────────────────────────────────── */}
      {step === 'done' && (
        <div style={{ textAlign: 'center', padding: '8px 0' }}>
          <div style={{
            width: 80, height: 80, borderRadius: 999, margin: '0 auto 20px',
            background: 'var(--sm-green-50)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 40,
          }}>
            ✅
          </div>
          <Button
            variant="dark" size="lg"
            onClick={() => navigate('/signin', { replace: true })}
            style={{ width: '100%' }}
          >
            Sign in with new password →
          </Button>
        </div>
      )}
    </AuthShell>
  )
}
