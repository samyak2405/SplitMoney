import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Icon from '../components/ui/Icon'

type PaymentOutcome = 'succeeded' | 'failed' | 'processing' | 'unknown'

const TERMINAL_BACKEND_STATUSES = new Set(['COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED', 'REFUND_INITIATED'])
const POLL_INTERVAL_MS = 2500
const POLL_MAX_ATTEMPTS = 24 // ~60 seconds

function resolveOutcomeFromUrl(status: string | null): PaymentOutcome {
  if (!status) return 'unknown'
  const s = status.toLowerCase()
  if (s === 'succeeded' || s === 'completed' || s === 'charged') return 'succeeded'
  if (s === 'failed' || s === 'cancelled' || s === 'voided') return 'failed'
  return 'processing'
}

function resolveOutcomeFromBackend(status: string): PaymentOutcome {
  if (status === 'COMPLETED') return 'succeeded'
  if (status === 'FAILED' || status === 'CANCELLED') return 'failed'
  if (status === 'REFUNDED' || status === 'REFUND_INITIATED') return 'failed'
  return 'processing'
}

async function fetchPaymentStatus(paymentId: string): Promise<string | null> {
  try {
    const res = await fetch(`/v1/payments/${paymentId}`, { credentials: 'include' })
    if (!res.ok) return null
    const data = await res.json()
    return data?.status ?? null
  } catch {
    return null
  }
}

export default function PaymentReturn() {
  const navigate = useNavigate()
  const [params] = useSearchParams()

  const urlStatus = params.get('status')
  const hyperswitchPaymentId = params.get('payment_id')
  const internalPaymentId = sessionStorage.getItem('pendingPaymentId')

  const [outcome, setOutcome] = useState<PaymentOutcome>(resolveOutcomeFromUrl(urlStatus))
  const [polling, setPolling] = useState(!!internalPaymentId)
  const attemptsRef = useRef(0)

  useEffect(() => {
    if (!internalPaymentId) return

    let cancelled = false

    const poll = async () => {
      if (cancelled) return
      attemptsRef.current += 1

      const status = await fetchPaymentStatus(internalPaymentId)

      if (cancelled) return

      if (status && TERMINAL_BACKEND_STATUSES.has(status)) {
        sessionStorage.removeItem('pendingPaymentId')
        setOutcome(resolveOutcomeFromBackend(status))
        setPolling(false)
        return
      }

      if (attemptsRef.current >= POLL_MAX_ATTEMPTS) {
        // Give up polling — fall back to URL param outcome
        sessionStorage.removeItem('pendingPaymentId')
        setPolling(false)
        return
      }

      setTimeout(poll, POLL_INTERVAL_MS)
    }

    setTimeout(poll, POLL_INTERVAL_MS)
    return () => { cancelled = true }
  }, [internalPaymentId])

  // Auto-redirect to dashboard after a confirmed success, with extra delay
  // so the async settlement callback has time to run before the user lands
  // on the group page and sees balances.
  useEffect(() => {
    if (outcome === 'succeeded' && !polling) {
      const t = setTimeout(() => navigate('/dashboard'), 5000)
      return () => clearTimeout(t)
    }
  }, [outcome, polling, navigate])

  return (
    <AppShell title="Payment result" subtitle="">
      <div style={{ maxWidth: 520, margin: '60px auto' }}>
        <Card style={{ padding: 40, textAlign: 'center' }}>

          {polling && (
            <>
              <div style={{
                width: 88, height: 88, borderRadius: 999,
                background: 'var(--bg3)',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 24,
              }}>
                <Icon name="clock" size={44} color="var(--fg2)" />
              </div>
              <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 36, fontWeight: 800, margin: 0 }}>
                Confirming payment…
              </h2>
              <p style={{ color: 'var(--fg2)', marginTop: 10, fontSize: 16 }}>
                Checking with the payment gateway. This takes a few seconds.
              </p>
            </>
          )}

          {!polling && outcome === 'succeeded' && (
            <>
              <div style={{
                width: 88, height: 88, borderRadius: 999,
                background: 'var(--sm-green-50)',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 24,
              }}>
                <Icon name="check" size={52} color="var(--sm-green-600)" stroke={3} />
              </div>
              <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 40, fontWeight: 800, margin: 0 }}>
                Payment successful
              </h2>
              <p style={{ color: 'var(--fg2)', marginTop: 10, fontSize: 16 }}>
                Your settlement has been confirmed. Balances will update in your group shortly.
              </p>
              {hyperswitchPaymentId && (
                <p style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 6, fontFamily: 'monospace' }}>
                  Ref: {hyperswitchPaymentId}
                </p>
              )}
              <p style={{ fontSize: 13, color: 'var(--fg3)', marginTop: 16 }}>
                Redirecting to dashboard in a moment…
              </p>
              <Button variant="primary" size="lg" onClick={() => navigate('/dashboard')} style={{ marginTop: 24, width: '100%' }}>
                Go to dashboard
              </Button>
            </>
          )}

          {!polling && outcome === 'failed' && (
            <>
              <div style={{
                width: 88, height: 88, borderRadius: 999,
                background: '#FFF0F0',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 24,
              }}>
                <Icon name="x" size={48} color="var(--sm-danger)" stroke={3} />
              </div>
              <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 40, fontWeight: 800, margin: 0 }}>
                Payment failed
              </h2>
              <p style={{ color: 'var(--fg2)', marginTop: 10, fontSize: 16 }}>
                Your payment could not be processed. No balances were changed.
              </p>
              <div style={{ display: 'flex', gap: 10, marginTop: 28 }}>
                <Button variant="ghost" size="lg" onClick={() => navigate('/dashboard')} style={{ flex: 1 }}>
                  Dashboard
                </Button>
                <Button variant="primary" size="lg" onClick={() => navigate('/settle')} style={{ flex: 1 }}>
                  Try again
                </Button>
              </div>
            </>
          )}

          {!polling && (outcome === 'processing' || outcome === 'unknown') && (
            <>
              <div style={{
                width: 88, height: 88, borderRadius: 999,
                background: 'var(--bg3)',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 24,
              }}>
                <Icon name="clock" size={44} color="var(--fg2)" />
              </div>
              <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 36, fontWeight: 800, margin: 0 }}>
                Payment pending
              </h2>
              <p style={{ color: 'var(--fg2)', marginTop: 10, fontSize: 16 }}>
                Your payment is being processed. We'll notify you when it's confirmed.
              </p>
              {hyperswitchPaymentId && (
                <p style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 6, fontFamily: 'monospace' }}>
                  Ref: {hyperswitchPaymentId}
                </p>
              )}
              <Button variant="primary" size="lg" onClick={() => navigate('/dashboard')} style={{ marginTop: 28, width: '100%' }}>
                Back to dashboard
              </Button>
            </>
          )}

        </Card>
      </div>
    </AppShell>
  )
}
