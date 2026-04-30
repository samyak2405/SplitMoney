/**
 * OtpModal — self-contained OTP verification modal.
 *
 * Props:
 *   open        boolean          — whether the modal is visible
 *   email       string           — email the OTP was/will be sent to
 *   mobile      string           — pre-filled mobile (optional)
 *   onSuccess   (email) => void  — called after OTP is verified successfully
 *   onClose     () => void       — called when user dismisses the modal
 */
import { useEffect, useRef, useState } from 'react'
import { resendRegistrationOtp, verifyRegistrationOtp } from '../../api/auth'
import { extractMessage } from '../../utils/messages'
import Icon from '../ui/Icon'
import Button from '../ui/Button'

const OTP_EXPIRY_DEFAULT_MS = 15 * 60 * 1000  // 15 minutes
const RESEND_COOLDOWN_MS    =      60 * 1000  //  1 minute

// ── helpers ──────────────────────────────────────────────────────────────────
function fmt2(n) { return String(n).padStart(2, '0') }

function fmtCountdown(seconds) {
  if (seconds == null || seconds <= 0) return '0:00'
  return `${Math.floor(seconds / 60)}:${fmt2(seconds % 60)}`
}

// ── sub-components ───────────────────────────────────────────────────────────
function Step({ n, label, active, done }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <div style={{
        width: 24, height: 24, borderRadius: 999, flexShrink: 0,
        background: done ? 'var(--sm-green-500)' : active ? 'var(--sm-ink-950)' : 'var(--bg4)',
        color: done || active ? 'white' : 'var(--fg3)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 12, fontWeight: 700,
      }}>
        {done ? <Icon name="check" size={12} color="white" stroke={3} /> : n}
      </div>
      <span style={{ fontSize: 13, fontWeight: 600, color: active ? 'var(--fg1)' : 'var(--fg3)' }}>{label}</span>
    </div>
  )
}

function OtpInput({ value, onChange, disabled }) {
  return (
    <input
      type="text"
      inputMode="numeric"
      maxLength={6}
      value={value}
      onChange={e => onChange(e.target.value.replace(/\D/g, '').slice(0, 6))}
      disabled={disabled}
      placeholder="000000"
      style={{
        fontFamily: 'var(--font-display)',
        fontWeight: 800,
        fontSize: 40,
        letterSpacing: '0.25em',
        textAlign: 'center',
        border: '2px solid var(--border-default)',
        borderRadius: 16,
        padding: '16px 20px',
        width: '100%',
        outline: 'none',
        background: disabled ? 'var(--bg3)' : 'white',
        color: 'var(--fg1)',
        transition: 'border-color 180ms',
        fontVariantNumeric: 'tabular-nums',
      }}
      onFocus={e => { e.target.style.borderColor = 'var(--sm-green-500)'; e.target.style.boxShadow = '0 0 0 4px rgba(18,179,94,.16)' }}
      onBlur={e  => { e.target.style.borderColor = 'var(--border-default)'; e.target.style.boxShadow = 'none' }}
    />
  )
}

// ── main component ────────────────────────────────────────────────────────────
export default function OtpModal({ open, email, mobile: mobileProp = '', onSuccess, onClose }) {
  // step: 'channel' | 'mobile' | 'verify'
  const [step,      setStep]      = useState('channel')
  const [channel,   setChannel]   = useState('EMAIL')
  const [mobile,    setMobile]    = useState(mobileProp || '')
  const [otpCode,   setOtpCode]   = useState('')
  const [error,     setError]     = useState('')
  const [info,      setInfo]      = useState('')

  const [isVerifying, setIsVerifying] = useState(false)
  const [isResending,  setIsResending]  = useState(false)

  const [otpExpiresAtMs,       setOtpExpiresAtMs]       = useState(null)
  const [resendAllowedAfterMs, setResendAllowedAfterMs] = useState(null)
  const [expiresInSec,         setExpiresInSec]         = useState(null)
  const [resendCooldownSec,    setResendCooldownSec]    = useState(null)

  const autoSentRef = useRef(false)

  // Reset all state when modal opens/closes
  useEffect(() => {
    if (!open) {
      setStep('channel')
      setChannel('EMAIL')
      setMobile(mobileProp || '')
      setOtpCode('')
      setError('')
      setInfo('')
      setOtpExpiresAtMs(null)
      setResendAllowedAfterMs(null)
      autoSentRef.current = false
      return
    }
    setMobile(mobileProp || '')
  }, [open, mobileProp])

  // Countdown timers
  useEffect(() => {
    if (!open) { setExpiresInSec(null); setResendCooldownSec(null); return }
    const tick = () => {
      setExpiresInSec(otpExpiresAtMs
        ? Math.max(0, Math.ceil((otpExpiresAtMs - Date.now()) / 1000))
        : null)
      setResendCooldownSec(resendAllowedAfterMs
        ? Math.max(0, Math.ceil((resendAllowedAfterMs - Date.now()) / 1000))
        : null)
    }
    tick()
    const id = window.setInterval(tick, 1000)
    return () => window.clearInterval(id)
  }, [open, otpExpiresAtMs, resendAllowedAfterMs])

  // Auto-send OTP when we enter the verify step for the first time
  useEffect(() => {
    if (!open || step !== 'verify' || otpExpiresAtMs != null || autoSentRef.current) return
    const normalizedEmail = email?.trim().toLowerCase()
    if (!normalizedEmail) return
    if (channel === 'SMS') {
      const m = mobile.trim()
      if (!m || !/^\+?[1-9]\d{7,14}$/.test(m)) return
    }
    autoSentRef.current = true
    let cancelled = false
    ;(async () => {
      try {
        const result = await resendRegistrationOtp({
          email: normalizedEmail,
          mobile: channel === 'SMS' ? (mobile.trim() || null) : null,
          otpChannel: channel,
        })
        if (cancelled) return
        if (!result.body?.success) {
          autoSentRef.current = false
          setError(extractMessage(result.body, 'Unable to send OTP.'))
          return
        }
        const expAt = result.body?.data?.otpExpiresAt
        const parsed = expAt ? new Date(expAt).getTime() : NaN
        setOtpExpiresAtMs(Number.isFinite(parsed) ? parsed : Date.now() + OTP_EXPIRY_DEFAULT_MS)
        setResendAllowedAfterMs(Date.now() + RESEND_COOLDOWN_MS)
        setInfo(`OTP sent to ${channel === 'EMAIL' ? normalizedEmail : mobile.trim()}.`)
      } catch {
        if (!cancelled) { autoSentRef.current = false; setError('Network error while sending OTP.') }
      }
    })()
    return () => { cancelled = true }
  }, [open, step, otpExpiresAtMs, email, mobile, channel])

  // ── handlers ────────────────────────────────────────────────────────────────
  const gotoVerify = (ch, mob = mobile) => {
    setChannel(ch)
    setMobile(mob)
    setStep('verify')
    setOtpCode('')
    setError('')
    setInfo('')
    setOtpExpiresAtMs(null)
    setResendAllowedAfterMs(null)
    autoSentRef.current = false
  }

  const handleChannelSelect = (ch) => {
    setError('')
    if (ch === 'EMAIL') {
      gotoVerify('EMAIL')
    } else {
      const m = mobile.trim()
      if (!m) { setChannel('SMS'); setStep('mobile'); return }
      if (!/^\+?[1-9]\d{7,14}$/.test(m)) {
        setError('Enter mobile in international format, e.g. +919876543210.')
        setChannel('SMS'); setStep('mobile'); return
      }
      gotoVerify('SMS', m)
    }
  }

  const handleMobileContinue = () => {
    const m = mobile.trim()
    if (!m) { setError('Please enter your mobile number.'); return }
    if (!/^\+?[1-9]\d{7,14}$/.test(m)) {
      setError('Enter mobile in international format, e.g. +919876543210.'); return
    }
    setError('')
    gotoVerify('SMS', m)
  }

  const handleResend = async () => {
    const normalizedEmail = email?.trim().toLowerCase()
    if (!normalizedEmail) { setError('Email is missing.'); return }
    const m = channel === 'SMS' ? mobile.trim() : null
    if (channel === 'SMS' && !m) { setError('Mobile number is missing.'); return }
    if (channel === 'SMS' && !/^\+?[1-9]\d{7,14}$/.test(m)) {
      setError('Enter mobile in international format, e.g. +919876543210.'); return
    }
    setError(''); setInfo('')
    setIsResending(true)
    try {
      const result = await resendRegistrationOtp({ email: normalizedEmail, mobile: m, otpChannel: channel })
      if (!result.body?.success) { setError(extractMessage(result.body, 'Unable to resend OTP.')); return }
      const expAt = result.body?.data?.otpExpiresAt
      const parsed = expAt ? new Date(expAt).getTime() : NaN
      setOtpExpiresAtMs(Number.isFinite(parsed) ? parsed : Date.now() + OTP_EXPIRY_DEFAULT_MS)
      setResendAllowedAfterMs(Date.now() + RESEND_COOLDOWN_MS)
      setInfo(result.body?.responseMessage || 'A new OTP has been sent.')
    } catch {
      setError('Network error while resending OTP.')
    } finally {
      setIsResending(false)
    }
  }

  const handleVerify = async (e) => {
    e.preventDefault()
    const normalizedEmail = email?.trim().toLowerCase()
    if (!normalizedEmail) { setError('Email is missing. Please sign up again.'); return }
    if (!/^\d{6}$/.test(otpCode)) { setError('Enter a valid 6-digit OTP.'); return }
    setError(''); setInfo('')
    setIsVerifying(true)
    try {
      const result = await verifyRegistrationOtp({ email: normalizedEmail, otp: otpCode })
      if (!result.body?.success) { setError(extractMessage(result.body, 'Unable to verify OTP.')); return }
      onSuccess?.(normalizedEmail)
    } catch {
      setError('Network error while verifying OTP.')
    } finally {
      setIsVerifying(false)
    }
  }

  // ── render ───────────────────────────────────────────────────────────────────
  if (!open) return null

  const stepIndex = step === 'channel' ? 0 : step === 'mobile' ? 1 : 2
  const canResend = !isResending && (!resendCooldownSec || resendCooldownSec <= 0)

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, zIndex: 200,
        background: 'rgba(11,21,18,0.55)', backdropFilter: 'blur(8px)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20,
      }}
    >
      <div
        onClick={e => e.stopPropagation()}
        style={{
          background: 'white', borderRadius: 24, maxWidth: 460, width: '100%',
          boxShadow: 'var(--shadow-xl)', overflow: 'hidden',
          animation: 'slideUp 220ms var(--ease-out) both',
        }}
      >
        {/* Header */}
        <div style={{ padding: '24px 28px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, letterSpacing: '-0.01em' }}>
              Verify your account
            </h2>
            <p style={{ margin: '6px 0 0', fontSize: 13, color: 'var(--fg3)' }}>{email}</p>
          </div>
          <button
            onClick={onClose}
            style={{ width: 36, height: 36, borderRadius: 12, border: 'none', background: 'var(--bg3)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}
          >
            <Icon name="close" size={18} />
          </button>
        </div>

        {/* Steps indicator */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '16px 28px', borderBottom: '1px solid var(--border-subtle)' }}>
          <Step n={1} label="Choose channel" active={stepIndex === 0} done={stepIndex > 0} />
          <div style={{ flex: 1, height: 2, borderRadius: 1, background: stepIndex > 0 ? 'var(--sm-green-500)' : 'var(--bg4)', transition: 'background 200ms' }} />
          <Step n={2} label="Enter OTP" active={stepIndex >= 1} done={stepIndex >= 2} />
        </div>

        <div style={{ padding: '24px 28px 28px' }}>
          {/* Error / info banners */}
          {error && (
            <div style={{ background: '#FFE5E5', color: '#C22', borderRadius: 12, padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16 }}>
              {error}
            </div>
          )}
          {info && !error && (
            <div style={{ background: 'var(--sm-green-50)', color: 'var(--sm-green-700)', borderRadius: 12, padding: '10px 14px', fontSize: 13, fontWeight: 500, marginBottom: 16 }}>
              {info}
            </div>
          )}

          {/* ── CHANNEL STEP ───────────────────────────────────────────── */}
          {step === 'channel' && (
            <>
              <p style={{ margin: '0 0 20px', fontSize: 14, color: 'var(--fg2)' }}>
                Where should we send the 6-digit code?
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <ChannelOption
                  icon="mail"
                  label="Email"
                  sub={email}
                  onClick={() => handleChannelSelect('EMAIL')}
                />
                <ChannelOption
                  icon="user"
                  label="SMS"
                  sub={mobile || 'Enter mobile number'}
                  onClick={() => handleChannelSelect('SMS')}
                />
              </div>
            </>
          )}

          {/* ── MOBILE STEP ────────────────────────────────────────────── */}
          {step === 'mobile' && (
            <>
              <p style={{ margin: '0 0 16px', fontSize: 14, color: 'var(--fg2)' }}>
                Enter your mobile number to receive the OTP via SMS.
              </p>
              <input
                type="tel"
                value={mobile}
                onChange={e => { setMobile(e.target.value); setError('') }}
                placeholder="+919876543210"
                style={{
                  fontFamily: 'var(--font-sans)', fontSize: 16, padding: '12px 14px',
                  borderRadius: 12, border: '1.5px solid var(--border-default)',
                  outline: 'none', width: '100%', marginBottom: 16,
                }}
                onFocus={e => { e.target.style.borderColor = 'var(--sm-green-500)'; e.target.style.boxShadow = '0 0 0 4px rgba(18,179,94,.16)' }}
                onBlur={e  => { e.target.style.borderColor = 'var(--border-default)'; e.target.style.boxShadow = 'none' }}
              />
              <div style={{ display: 'flex', gap: 10 }}>
                <Button variant="ghost" onClick={() => { setStep('channel'); setError('') }} style={{ flex: 1 }}>← Back</Button>
                <Button variant="dark" onClick={handleMobileContinue} style={{ flex: 2 }}>Continue →</Button>
              </div>
            </>
          )}

          {/* ── VERIFY STEP ────────────────────────────────────────────── */}
          {step === 'verify' && (
            <>
              <p style={{ margin: '0 0 20px', fontSize: 14, color: 'var(--fg2)' }}>
                {channel === 'EMAIL'
                  ? `We sent a 6-digit code to ${email}.`
                  : `We sent a 6-digit code to ${mobile}.`}
              </p>

              <form onSubmit={handleVerify}>
                <OtpInput value={otpCode} onChange={v => { setOtpCode(v); setError('') }} disabled={isVerifying} />

                {/* Timers row */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '12px 0 20px', fontSize: 12, color: 'var(--fg3)' }}>
                  <span>
                    {expiresInSec != null && expiresInSec > 0
                      ? `Expires in ${fmtCountdown(expiresInSec)}`
                      : expiresInSec === 0
                      ? 'OTP expired — resend a new one'
                      : null}
                  </span>
                  <button
                    type="button"
                    onClick={handleResend}
                    disabled={!canResend}
                    style={{
                      background: 'none', border: 'none', cursor: canResend ? 'pointer' : 'not-allowed',
                      color: canResend ? 'var(--sm-green-600)' : 'var(--fg3)',
                      fontWeight: 600, fontSize: 12,
                    }}
                  >
                    {isResending
                      ? 'Sending…'
                      : resendCooldownSec && resendCooldownSec > 0
                      ? `Resend in ${resendCooldownSec}s`
                      : 'Resend OTP'}
                  </button>
                </div>

                <Button
                  type="submit"
                  variant="dark"
                  size="lg"
                  style={{ width: '100%' }}
                  disabled={otpCode.length !== 6 || isVerifying}
                >
                  {isVerifying ? 'Verifying…' : 'Verify →'}
                </Button>
              </form>

              <button
                onClick={() => { setStep('channel'); setError(''); setOtpCode(''); setOtpExpiresAtMs(null); setResendAllowedAfterMs(null); autoSentRef.current = false }}
                style={{ background: 'none', border: 'none', color: 'var(--fg3)', cursor: 'pointer', marginTop: 14, fontSize: 13, width: '100%' }}
              >
                ← Change delivery channel
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function ChannelOption({ icon, label, sub, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex', alignItems: 'center', gap: 14, padding: '16px 18px',
        borderRadius: 14, border: '1.5px solid var(--border-default)', background: 'white',
        cursor: 'pointer', textAlign: 'left', width: '100%',
        transition: 'all 150ms',
      }}
      onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--sm-green-500)'; e.currentTarget.style.background = 'var(--sm-green-50)' }}
      onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border-default)'; e.currentTarget.style.background = 'white' }}
    >
      <div style={{ width: 44, height: 44, borderRadius: 12, background: 'var(--sm-green-50)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        <Icon name={icon} size={20} color="var(--sm-green-600)" />
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontWeight: 600, fontSize: 15 }}>{label}</div>
        <div style={{ fontSize: 12, color: 'var(--fg3)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{sub}</div>
      </div>
      <Icon name="chevronRight" size={16} color="var(--fg3)" style={{ marginLeft: 'auto', flexShrink: 0 }} />
    </button>
  )
}
