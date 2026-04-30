import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import Icon from '../components/ui/Icon'
import Field from '../components/ui/Field'
import { getUserGroups, getUserBalances, settleExpense } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import { extractMessage } from '../utils/messages'
import { generateId } from '../utils/requestMeta'
import type { BalanceEntry, Group, PaymentInitiationResponse } from '../types'

type PaymentMethod = 'CARD' | 'UPI'

const RETURN_URL = `${window.location.origin}/payment/return`

function fmt(n: number | string) {
  const abs = Math.abs(Number(n) || 0).toFixed(2)
  const [w, f] = abs.split('.')
  return `₹${Number(w).toLocaleString()}.${f}`
}

const PAYMENT_METHODS: { k: PaymentMethod; label: string; sub: string; icon: string }[] = [
  { k: 'CARD', label: 'Credit / Debit card', sub: 'Visa, Mastercard, RuPay — powered by Hyperswitch', icon: '💳' },
  { k: 'UPI',  label: 'UPI',                 sub: 'PhonePe, GPay, Paytm — instant transfer',          icon: '📲' },
]

export default function SettleUp() {
  const navigate = useNavigate()
  const location = useLocation()
  const { handleUnauthorized, session } = useAuth()

  const navState = location.state || {}

  const [step, setStep] = useState(1)
  const [method, setMethod] = useState<PaymentMethod>('CARD')

  // Group selection
  const [groups, setGroups] = useState<Group[]>([])
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(navState.groupId || null)
  const [selectedGroupName, setSelectedGroupName] = useState<string>(navState.groupName || '')
  const [selectedCurrency, setSelectedCurrency] = useState<string>(navState.currency || 'INR')
  const [loadingGroups, setLoadingGroups] = useState(!navState.groupId)

  // Balances
  const [owedList, setOwedList] = useState<BalanceEntry[]>([])
  const [loadingBalances, setLoadingBalances] = useState(false)
  const [balancesError, setBalancesError] = useState<string | null>(null)

  // Step selections
  const [selectedPersonEmail, setSelectedPersonEmail] = useState<string | null>(null)
  const [amount, setAmount] = useState(0)

  // Submit state
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [initiatedPayment, setInitiatedPayment] = useState<PaymentInitiationResponse | null>(null)

  useEffect(() => {
    if (navState.groupId) return
    let cancelled = false
    const load = async () => {
      const result = await getUserGroups()
      if (cancelled) return
      if (result.status === 401) { handleUnauthorized(result.body); return }
      if (result.ok && result.body?.data?.groups) {
        const gs = result.body.data.groups
        setGroups(gs)
        if (gs.length > 0) {
          setSelectedGroupId(gs[0].groupId)
          setSelectedGroupName(gs[0].groupName)
          setSelectedCurrency(gs[0].currency || 'INR')
        }
      }
      setLoadingGroups(false)
    }
    load()
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!selectedGroupName) return
    let cancelled = false
    const load = async () => {
      setLoadingBalances(true)
      setBalancesError(null)
      const result = await getUserBalances({ groupName: selectedGroupName })
      if (cancelled) return
      if (result.status === 401) { handleUnauthorized(result.body); return }
      if (result.ok && result.body?.data) {
        const d = result.body.data
        setOwedList(d.membersUserNeedsToPay || [])
        if (d.membersUserNeedsToPay?.length > 0) {
          setSelectedPersonEmail(d.membersUserNeedsToPay[0].email)
          setAmount(parseFloat(d.membersUserNeedsToPay[0].amount) || 0)
        }
      } else {
        setBalancesError(extractMessage(result.body, 'Failed to load balances'))
      }
      setLoadingBalances(false)
    }
    load()
    return () => { cancelled = true }
  }, [selectedGroupName])

  const handleGroupChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const gid = Number(e.target.value)
    const g = groups.find(x => x.groupId === gid)
    if (g) {
      setSelectedGroupId(g.groupId)
      setSelectedGroupName(g.groupName)
      setSelectedCurrency(g.currency || 'INR')
    }
  }

  const handlePay = async () => {
    if (!selectedPersonEmail || !selectedGroupId || !session?.email) return
    setSubmitting(true)
    setSubmitError(null)

    const result = await settleExpense({
      groupId: selectedGroupId,
      paidByEmail: session.email,
      paidTo: selectedPersonEmail,
      amount: amount.toFixed(2),
      currency: selectedCurrency,
      paymentMethod: method,
      returnUrl: RETURN_URL,
      idempotencyKey: generateId(),
    })

    setSubmitting(false)
    if (result.status === 401) { handleUnauthorized(result.body); return }

    if (result.ok && result.body?.data) {
      const data = result.body.data as PaymentInitiationResponse
      setInitiatedPayment(data)

      if (data.checkout_url) {
        // Store internal payment ID so PaymentReturn can poll the backend
        sessionStorage.setItem('pendingPaymentId', data.payment_id)
        window.location.href = data.checkout_url
      } else if (method === 'UPI') {
        // UPI direct debit — no hosted checkout needed
        setStep(4)
      } else {
        // CARD with no checkout URL means the publishable key is not configured
        setSubmitError(
          'Payment gateway checkout is not configured. Set APP_HYPERSWITCH_PUBLISHABLE_KEY in the payment service.'
        )
      }
    } else {
      setSubmitError(extractMessage(result.body, 'Failed to initiate payment'))
    }
  }

  const recipient = owedList.find(f => f.email === selectedPersonEmail)
  const steps = ['Who', 'How much', 'Pay', 'Done']

  const BackBtn = (
    <button
      onClick={() => navigate(selectedGroupName ? '/groups/' + encodeURIComponent(selectedGroupName) : '/dashboard')}
      style={{
        width: 40, height: 40, borderRadius: 12,
        border: '1.5px solid var(--border-default)',
        background: 'white', cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
    >
      <Icon name="chevronLeft" size={18} />
    </button>
  )

  return (
    <AppShell back={BackBtn} title="Settle up" subtitle="Pay a friend directly via card or UPI.">
      <div style={{ maxWidth: 760, margin: '0 auto' }}>
        {/* Step indicator */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 24 }}>
          {steps.map((l, i) => (
            <div key={l} style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  width: 28, height: 28, borderRadius: 999, flexShrink: 0,
                  background: step > i ? 'var(--sm-green-500)' : 'var(--bg4)',
                  color: step > i ? 'white' : 'var(--fg3)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 13, fontWeight: 700, transition: 'all 220ms',
                }}
              >
                {step > i + 1 ? <Icon name="check" size={14} color="white" /> : i + 1}
              </div>
              <span style={{ fontSize: 13, fontWeight: 600, color: step > i ? 'var(--fg1)' : 'var(--fg3)', transition: 'color 220ms' }}>
                {l}
              </span>
              {i < steps.length - 1 && (
                <div style={{ flex: 1, height: 2, background: step > i + 1 ? 'var(--sm-green-500)' : 'var(--bg4)', transition: 'background 220ms' }} />
              )}
            </div>
          ))}
        </div>

        <Card style={{ padding: 32 }}>
          {/* Step 1: Who */}
          {step === 1 && (
            <>
              <h2 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 600, letterSpacing: '-0.01em' }}>
                Who do you want to pay?
              </h2>
              <p style={{ color: 'var(--fg2)', margin: '0 0 20px' }}>Sorted by how much you owe.</p>

              {!navState.groupId && (
                <div style={{ marginBottom: 20 }}>
                  <Field label="Group">
                    {loadingGroups ? (
                      <div style={{ color: 'var(--fg3)', fontSize: 13 }}>Loading groups…</div>
                    ) : (
                      <select
                        value={selectedGroupId || ''}
                        onChange={handleGroupChange}
                        style={{ padding: '12px 14px', borderRadius: 12, border: '1.5px solid var(--border-default)', fontSize: 15, fontFamily: 'var(--font-sans)', width: '100%', outline: 'none' }}
                      >
                        {groups.map(g => (
                          <option key={g.groupId} value={g.groupId}>{g.groupName}</option>
                        ))}
                      </select>
                    )}
                  </Field>
                </div>
              )}

              {balancesError && (
                <div style={{ background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)', padding: '12px 16px', borderRadius: 12, marginBottom: 16, fontSize: 14 }}>
                  {balancesError}
                </div>
              )}

              {loadingBalances && <div style={{ color: 'var(--fg3)', padding: '20px 0' }}>Loading balances…</div>}

              {!loadingBalances && owedList.length === 0 && !balancesError && (
                <div style={{ color: 'var(--fg3)', padding: '20px 0', textAlign: 'center' }}>
                  <div style={{ fontSize: 40, marginBottom: 8 }}>🎉</div>
                  You don't owe anyone in this group!
                </div>
              )}

              {!loadingBalances && owedList.map(f => (
                <label
                  key={f.email}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 14, padding: '14px 16px',
                    borderRadius: 14, cursor: 'pointer', marginBottom: 10,
                    border: selectedPersonEmail === f.email ? '2px solid var(--sm-green-500)' : '1.5px solid var(--border-subtle)',
                    background: selectedPersonEmail === f.email ? 'var(--sm-green-50)' : 'white',
                    transition: 'all 150ms',
                  }}
                >
                  <input
                    type="radio"
                    name="who"
                    checked={selectedPersonEmail === f.email}
                    onChange={() => { setSelectedPersonEmail(f.email); setAmount(parseFloat(f.amount) || 0) }}
                    style={{ accentColor: 'var(--sm-green-500)' }}
                  />
                  <Avatar name={f.email.split('@')[0]} size={44} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600 }}>{f.email.split('@')[0]}</div>
                    <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{f.email}</div>
                  </div>
                  <div style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', color: 'var(--sm-danger)' }}>
                    − {fmt(f.amount)}
                  </div>
                </label>
              ))}

              {owedList.length > 0 && (
                <Button variant="primary" size="lg" onClick={() => setStep(2)} style={{ width: '100%', marginTop: 12 }}>
                  Continue →
                </Button>
              )}
            </>
          )}

          {/* Step 2: How much */}
          {step === 2 && (
            <>
              <h2 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 600, letterSpacing: '-0.01em' }}>
                How much to pay {recipient?.email.split('@')[0]}?
              </h2>
              <p style={{ color: 'var(--fg2)', margin: '0 0 24px' }}>
                Full amount is {fmt(recipient?.amount || 0)}.
              </p>
              <div
                style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24,
                  borderRadius: 16, background: 'var(--sm-green-50)', marginBottom: 16,
                }}
              >
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 72, fontWeight: 800, color: 'var(--sm-green-700)' }}>₹</span>
                <input
                  type="number"
                  step="0.01"
                  value={amount}
                  onChange={e => setAmount(Number(e.target.value))}
                  style={{
                    border: 'none', background: 'transparent', outline: 'none',
                    fontFamily: 'var(--font-display)', fontSize: 72, fontWeight: 800,
                    letterSpacing: '-0.03em', width: 240, textAlign: 'center', color: 'var(--sm-green-700)',
                  }}
                />
              </div>
              <div style={{ display: 'flex', gap: 10 }}>
                <Button variant="ghost" size="lg" onClick={() => setStep(1)} style={{ flex: 1 }}>← Back</Button>
                <Button variant="primary" size="lg" onClick={() => setStep(3)} style={{ flex: 2 }}>Continue →</Button>
              </div>
            </>
          )}

          {/* Step 3: Pay method */}
          {step === 3 && (
            <>
              <h2 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 600, letterSpacing: '-0.01em' }}>
                How would you like to pay?
              </h2>
              <p style={{ color: 'var(--fg2)', margin: '0 0 20px' }}>
                Paying {fmt(amount)} to {recipient?.email.split('@')[0]}.
              </p>

              {submitError && (
                <div style={{ background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)', padding: '12px 16px', borderRadius: 12, marginBottom: 16, fontSize: 14 }}>
                  {submitError}
                </div>
              )}

              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {PAYMENT_METHODS.map(m => (
                  <label
                    key={m.k}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 14, padding: '16px 18px',
                      borderRadius: 14, cursor: 'pointer',
                      border: method === m.k ? '2px solid var(--sm-green-500)' : '1.5px solid var(--border-subtle)',
                      background: method === m.k ? 'var(--sm-green-50)' : 'white',
                      transition: 'all 150ms',
                    }}
                  >
                    <input type="radio" name="method" checked={method === m.k} onChange={() => setMethod(m.k)} style={{ accentColor: 'var(--sm-green-500)' }} />
                    <div style={{ width: 44, height: 44, borderRadius: 12, background: 'var(--bg3)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, flexShrink: 0 }}>
                      {m.icon}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600 }}>{m.label}</div>
                      <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{m.sub}</div>
                    </div>
                  </label>
                ))}
              </div>

              <div style={{ marginTop: 20, padding: '12px 16px', borderRadius: 12, background: 'var(--bg2)', border: '1px solid var(--border-subtle)', fontSize: 13, color: 'var(--fg3)' }}>
                You'll be redirected to a secure checkout page to complete your payment.
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                <Button variant="ghost" size="lg" onClick={() => setStep(2)} style={{ flex: 1 }}>← Back</Button>
                <Button variant="primary" size="lg" onClick={handlePay} disabled={submitting} style={{ flex: 2 }}>
                  {submitting ? 'Initiating…' : `Pay ${fmt(amount)}`}
                </Button>
              </div>
            </>
          )}

          {/* Step 4: Done (shown only if no checkoutUrl, e.g. UPI direct debit path) */}
          {step === 4 && (
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <div style={{ width: 96, height: 96, borderRadius: 999, background: 'var(--sm-green-50)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: 24 }}>
                <Icon name="check" size={56} color="var(--sm-green-600)" stroke={3} />
              </div>
              <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 44, fontWeight: 800, letterSpacing: '-0.02em', margin: 0 }}>
                Payment initiated!
              </h2>
              <p style={{ color: 'var(--fg2)', fontSize: 17, marginTop: 10 }}>
                Your {method} payment to <b>{recipient?.email.split('@')[0]}</b> for <b>{fmt(amount)}</b> is being processed.
              </p>
              {initiatedPayment?.payment_id && (
                <p style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 8, fontFamily: 'monospace' }}>
                  Payment ID: {initiatedPayment.payment_id}
                </p>
              )}
              <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 32 }}>
                {selectedGroupName && (
                  <Button variant="ghost" size="lg" onClick={() => navigate('/groups/' + encodeURIComponent(selectedGroupName))}>
                    Back to group
                  </Button>
                )}
                <Button variant="primary" size="lg" onClick={() => navigate('/dashboard')}>
                  Done
                </Button>
              </div>
            </div>
          )}
        </Card>
      </div>
    </AppShell>
  )
}
