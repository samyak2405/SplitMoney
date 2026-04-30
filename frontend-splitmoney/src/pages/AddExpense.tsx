import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import Field from '../components/ui/Field'
import Input from '../components/ui/Input'
import Icon from '../components/ui/Icon'
import { CATEGORIES } from '../data'
import { getUserGroups, getGroupDetails, addExpense } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import { extractMessage } from '../utils/messages'
import { generateId } from '../utils/requestMeta'

const SPLIT_TYPE_MAP = { equal: 'EQUAL', custom: 'EXACT', percent: 'PERCENTAGE' }

function fmt(n) {
  const abs = Math.abs(Number(n) || 0).toFixed(2)
  const [w, f] = abs.split('.')
  return `₹${Number(w).toLocaleString()}.${f}`
}

export default function AddExpense() {
  const navigate = useNavigate()
  const location = useLocation()
  const { handleUnauthorized, session } = useAuth()

  const navState = location.state || {}

  const [amount, setAmount] = useState('0.00')
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('food')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [splitMode, setSplitMode] = useState('equal')

  const [groups, setGroups] = useState([])
  const [selectedGroupId, setSelectedGroupId] = useState(navState.groupId || null)
  const [selectedGroupName, setSelectedGroupName] = useState(navState.groupName || '')
  const [selectedCurrency, setSelectedCurrency] = useState(navState.currency || 'INR')

  const [members, setMembers] = useState([])
  const [paidByEmail, setPaidByEmail] = useState(session?.email || '')
  const [selectedMembers, setSelectedMembers] = useState([])

  // per-member custom split values: { [email]: string }
  const [exactAmounts, setExactAmounts] = useState({})
  const [percentages, setPercentages] = useState({})

  const [loadingGroups, setLoadingGroups] = useState(true)
  const [loadingMembers, setLoadingMembers] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  // Load groups on mount
  useEffect(() => {
    let cancelled = false
    const load = async () => {
      const result = await getUserGroups()
      if (cancelled) return
      if (result.status === 401) { handleUnauthorized(result.body); return }
      if (result.ok && result.body?.data?.groups) {
        const gs = result.body.data.groups
        setGroups(gs)
        // pre-select from nav state or first group
        if (!selectedGroupId && gs.length > 0) {
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

  // Load members when group changes
  useEffect(() => {
    if (!selectedGroupName) return
    let cancelled = false
    const load = async () => {
      setLoadingMembers(true)
      const result = await getGroupDetails({ groupName: selectedGroupName })
      if (cancelled) return
      if (result.status === 401) { handleUnauthorized(result.body); return }
      if (result.ok && result.body?.data?.members) {
        const ms = result.body.data.members
        setMembers(ms)
        setSelectedMembers(ms.map(m => m.email))
        // default paid-by to current user if they're a member, else first member
        const selfMember = ms.find(m => m.email === session?.email)
        setPaidByEmail(selfMember ? selfMember.email : (ms[0]?.email || ''))
      }
      setLoadingMembers(false)
    }
    load()
    return () => { cancelled = true }
  }, [selectedGroupName])

  const handleGroupChange = (e) => {
    const gid = Number(e.target.value)
    const g = groups.find(x => x.groupId === gid)
    if (g) {
      setSelectedGroupId(g.groupId)
      setSelectedGroupName(g.groupName)
      setSelectedCurrency(g.currency || 'INR')
    }
  }

  const toggleMember = (email) => {
    setSelectedMembers(prev =>
      prev.includes(email) ? prev.filter(e => e !== email) : [...prev, email]
    )
  }

  const perPerson = selectedMembers.length > 0 ? Number(amount) / selectedMembers.length : 0

  const buildParticipants = () => {
    if (splitMode === 'equal') {
      return selectedMembers.map(email => ({ email }))
    }
    if (splitMode === 'custom') {
      return selectedMembers.map(email => ({ email, exact_amount: exactAmounts[email] || '0' }))
    }
    if (splitMode === 'percent') {
      return selectedMembers.map(email => ({ email, percentage: percentages[email] || '0' }))
    }
    return selectedMembers.map(email => ({ email }))
  }

  const handleSubmit = async () => {
    if (!title.trim()) { setError('Description is required'); return }
    if (!amount || Number(amount) <= 0) { setError('Amount must be greater than 0'); return }
    if (!selectedGroupId) { setError('Please select a group'); return }
    if (selectedMembers.length === 0) { setError('Select at least one member'); return }

    setSubmitting(true)
    setError(null)

    const result = await addExpense({
      groupId: selectedGroupId,
      paidByEmail,
      totalAmount: amount,
      currency: selectedCurrency,
      description: title.trim(),
      expenseDate: date ? `${date}T00:00:00` : null,
      splitType: SPLIT_TYPE_MAP[splitMode] || 'EQUAL',
      participants: buildParticipants(),
      idempotencyKey: generateId(),
    })

    setSubmitting(false)
    if (result.status === 401) { handleUnauthorized(result.body); return }
    if (result.ok) {
      navigate('/groups/' + encodeURIComponent(selectedGroupName))
    } else {
      setError(extractMessage(result.body, 'Failed to save expense'))
    }
  }

  const BackBtn = (
    <button
      onClick={() => navigate(navState.groupName ? '/groups/' + encodeURIComponent(navState.groupName) : '/dashboard')}
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
    <AppShell back={BackBtn} title="Add an expense" subtitle="We'll split it automatically.">
      <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 24, maxWidth: 1100 }}>
        {/* Main form */}
        <Card style={{ padding: 32 }}>
          {error && (
            <div style={{
              background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)',
              padding: '12px 16px', borderRadius: 12, marginBottom: 16, fontSize: 14,
            }}>
              {error}
            </div>
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Amount */}
            <Field label="Amount">
              <div
                style={{
                  display: 'flex', alignItems: 'center', gap: 8, padding: '4px 14px',
                  borderRadius: 16, border: '2px solid var(--sm-green-500)', background: 'var(--sm-green-50)',
                }}
              >
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 48, fontWeight: 800, color: 'var(--sm-green-700)' }}>
                  {selectedCurrency === 'INR' ? '₹' : selectedCurrency === 'EUR' ? '€' : selectedCurrency === 'GBP' ? '£' : '$'}
                </span>
                <input
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  style={{
                    flex: 1, border: 'none', background: 'transparent', outline: 'none',
                    fontFamily: 'var(--font-display)', fontSize: 48, fontWeight: 800,
                    letterSpacing: '-0.02em', color: 'var(--fg1)', fontVariantNumeric: 'tabular-nums',
                  }}
                />
                <span style={{ fontWeight: 700, fontSize: 16, color: 'var(--fg2)' }}>{selectedCurrency}</span>
              </div>
            </Field>

            {/* Title */}
            <Field label="What was it for?">
              <Input
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="Dinner, Uber, groceries…"
              />
            </Field>

            {/* Category */}
            <Field label="Category">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
                {CATEGORIES.map(c => (
                  <button
                    key={c.key}
                    onClick={() => setCategory(c.key)}
                    style={{
                      padding: 12, borderRadius: 14, cursor: 'pointer',
                      border: category === c.key ? '2px solid var(--sm-green-500)' : '1.5px solid var(--border-default)',
                      background: category === c.key ? 'var(--sm-green-50)' : 'white',
                      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
                      transition: 'all 150ms',
                    }}
                  >
                    <div style={{ width: 36, height: 36, borderRadius: 10, background: c.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
                      {c.emoji}
                    </div>
                    <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--fg1)' }}>{c.label}</div>
                  </button>
                ))}
              </div>
            </Field>

            {/* Group */}
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

            {/* Paid by + Date */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              <Field label="Paid by">
                {loadingMembers ? (
                  <div style={{ color: 'var(--fg3)', fontSize: 13 }}>Loading…</div>
                ) : (
                  <select
                    value={paidByEmail}
                    onChange={e => setPaidByEmail(e.target.value)}
                    style={{ padding: '12px 14px', borderRadius: 12, border: '1.5px solid var(--border-default)', fontSize: 15, fontFamily: 'var(--font-sans)', width: '100%', outline: 'none' }}
                  >
                    {members.map(m => (
                      <option key={m.email} value={m.email}>
                        {m.email === session?.email ? 'You' : m.email.split('@')[0]}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label="Date">
                <Input type="date" value={date} onChange={e => setDate(e.target.value)} />
              </Field>
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
              <Button variant="primary" size="lg" onClick={handleSubmit} disabled={submitting} style={{ flex: 1 }}>
                {submitting ? 'Saving…' : 'Save expense'}
              </Button>
              <Button variant="ghost" size="lg" onClick={() => navigate(navState.groupName ? '/groups/' + encodeURIComponent(navState.groupName) : '/dashboard')}>
                Cancel
              </Button>
            </div>
          </div>
        </Card>

        {/* Right panel: split */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Card style={{ padding: 24 }}>
            <div style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--fg3)', fontWeight: 600 }}>
              Split between
            </div>
            <div style={{ display: 'flex', gap: 6, margin: '12px 0 16px', padding: 4, background: 'var(--bg3)', borderRadius: 12 }}>
              {[['equal', 'Equal'], ['custom', 'Exact'], ['percent', '%']].map(([k, l]) => (
                <button
                  key={k}
                  onClick={() => setSplitMode(k)}
                  style={{
                    flex: 1, padding: '8px', border: 'none', borderRadius: 8, cursor: 'pointer',
                    background: splitMode === k ? 'white' : 'transparent',
                    boxShadow: splitMode === k ? 'var(--shadow-xs)' : 'none',
                    fontSize: 12, fontWeight: 600,
                    color: splitMode === k ? 'var(--fg1)' : 'var(--fg3)',
                    transition: 'all 150ms',
                  }}
                >
                  {l}
                </button>
              ))}
            </div>

            {loadingMembers ? (
              <div style={{ color: 'var(--fg3)', fontSize: 13 }}>Loading members…</div>
            ) : (
              members.map(m => (
                <div
                  key={m.email}
                  style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0', borderBottom: '1px solid var(--border-subtle)' }}
                >
                  <input
                    type="checkbox"
                    checked={selectedMembers.includes(m.email)}
                    onChange={() => toggleMember(m.email)}
                    style={{ width: 18, height: 18, accentColor: 'var(--sm-green-500)' }}
                  />
                  <Avatar name={m.email.split('@')[0]} size={32} />
                  <div style={{ flex: 1, fontSize: 14, fontWeight: 500 }}>
                    {m.email === session?.email ? 'You' : m.email.split('@')[0]}
                  </div>
                  {splitMode === 'equal' && (
                    <div style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600, fontSize: 14, color: 'var(--fg2)' }}>
                      {selectedMembers.includes(m.email) ? fmt(perPerson) : '—'}
                    </div>
                  )}
                  {splitMode === 'custom' && selectedMembers.includes(m.email) && (
                    <input
                      type="number"
                      value={exactAmounts[m.email] || ''}
                      onChange={e => setExactAmounts(prev => ({ ...prev, [m.email]: e.target.value }))}
                      placeholder="0.00"
                      style={{ width: 80, padding: '4px 8px', borderRadius: 8, border: '1.5px solid var(--border-default)', fontSize: 13, textAlign: 'right' }}
                    />
                  )}
                  {splitMode === 'percent' && selectedMembers.includes(m.email) && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                      <input
                        type="number"
                        value={percentages[m.email] || ''}
                        onChange={e => setPercentages(prev => ({ ...prev, [m.email]: e.target.value }))}
                        placeholder="0"
                        style={{ width: 64, padding: '4px 8px', borderRadius: 8, border: '1.5px solid var(--border-default)', fontSize: 13, textAlign: 'right' }}
                      />
                      <span style={{ fontSize: 13, color: 'var(--fg3)' }}>%</span>
                    </div>
                  )}
                </div>
              ))
            )}
          </Card>

          {/* Summary */}
          <Card style={{ padding: 24, background: 'var(--sm-ink-950)', color: 'white', border: 'none', position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', inset: '-30%', background: 'var(--grad-glow)', opacity: 0.6 }} />
            <div style={{ position: 'relative' }}>
              <div style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em', opacity: 0.6 }}>Summary</div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 12 }}>
                <span style={{ opacity: 0.7 }}>Total</span>
                <span style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', fontSize: 18 }}>{selectedCurrency} {amount}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 8 }}>
                <span style={{ opacity: 0.7 }}>Split</span>
                <span style={{ fontWeight: 600, fontSize: 14, opacity: 0.8 }}>{selectedMembers.length} members</span>
              </div>
              {splitMode === 'equal' && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 8, paddingTop: 12, borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                  <span style={{ opacity: 0.7 }}>Per person</span>
                  <span style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', fontSize: 18, color: '#D8FF5E' }}>{fmt(perPerson)}</span>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </AppShell>
  )
}
