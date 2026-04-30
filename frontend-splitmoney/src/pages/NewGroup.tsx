import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Field from '../components/ui/Field'
import Input from '../components/ui/Input'
import Icon from '../components/ui/Icon'
import { createGroup } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import { extractMessage } from '../utils/messages'

const EMOJIS = ['✈️', '🏠', '🎿', '📚', '🍕', '🎉', '💑', '🛒', '🚗', '🎬', '🏖️', '🎁']
const TYPES = ['Trip', 'Home', 'Couple', 'Other']

const CURRENCY_MAP = {
  'INR — Indian Rupee': 'INR',
  'EUR — Euro': 'EUR',
  'GBP — British Pound': 'GBP',
  'USD — US Dollar': 'USD',
}

export default function NewGroup() {
  const navigate = useNavigate()
  const { handleUnauthorized } = useAuth()

  const [name, setName] = useState('')
  const [emoji, setEmoji] = useState('✈️')
  const [type, setType] = useState('Trip')
  const [currency, setCurrency] = useState('INR — Indian Rupee')
  const [memberInputs, setMemberInputs] = useState([{ email: '', phone: '' }])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const updateMember = (index, field, value) => {
    setMemberInputs(prev => prev.map((m, i) => i === index ? { ...m, [field]: value } : m))
  }

  const addMemberRow = () => {
    setMemberInputs(prev => [...prev, { email: '', phone: '' }])
  }

  const removeMemberRow = (index) => {
    setMemberInputs(prev => prev.filter((_, i) => i !== index))
  }

  const handleSubmit = async () => {
    if (!name.trim()) { setError('Group name is required'); return }
    const validMembers = memberInputs
      .filter(m => m.email.trim())
      .map(m => ({ email: m.email.trim(), phoneNumber: m.phone.trim() || null }))
    if (validMembers.length === 0) { setError('Add at least one member email'); return }

    setLoading(true)
    setError(null)
    const result = await createGroup({
      name: name.trim(),
      description: type,
      currency: CURRENCY_MAP[currency] || 'INR',
      members: validMembers,
    })
    setLoading(false)

    if (result.status === 401) { handleUnauthorized(result.body); return }
    if (result.ok && result.body?.data?.groupName) {
      navigate('/groups/' + encodeURIComponent(result.body.data.groupName))
    } else {
      setError(extractMessage(result.body, 'Failed to create group'))
    }
  }

  const BackBtn = (
    <button
      onClick={() => navigate('/groups')}
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
    <AppShell back={BackBtn} title="Create a group" subtitle="Add members and start splitting.">
      <Card style={{ padding: 32, maxWidth: 640 }}>
        {error && (
          <div style={{
            background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)',
            padding: '12px 16px', borderRadius: 12, marginBottom: 16, fontSize: 14,
          }}>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', gap: 16, marginBottom: 20 }}>
          <div
            style={{
              width: 96, height: 96, borderRadius: 24, background: 'var(--sm-green-50)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 56, cursor: 'pointer',
              border: '2px dashed var(--sm-green-200)',
              flexShrink: 0,
            }}
          >
            {emoji}
          </div>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Field label="Group name">
              <Input
                value={name}
                onChange={e => setName(e.target.value)}
                placeholder="Lisbon weekend, Apartment 4B…"
              />
            </Field>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {EMOJIS.map(e => (
                <button
                  key={e}
                  onClick={() => setEmoji(e)}
                  style={{
                    width: 36, height: 36, border: 'none', cursor: 'pointer',
                    background: emoji === e ? 'var(--sm-green-50)' : 'transparent',
                    borderRadius: 10, fontSize: 20,
                    outline: emoji === e ? '2px solid var(--sm-green-500)' : '1.5px solid var(--border-default)',
                    transition: 'all 120ms',
                  }}
                >
                  {e}
                </button>
              ))}
            </div>
          </div>
        </div>

        <Field label="Group type">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
            {TYPES.map(t => (
              <button
                key={t}
                onClick={() => setType(t)}
                style={{
                  padding: '12px', borderRadius: 12, cursor: 'pointer',
                  border: type === t ? '2px solid var(--sm-green-500)' : '1.5px solid var(--border-default)',
                  background: type === t ? 'var(--sm-green-50)' : 'white',
                  fontWeight: 600, fontSize: 13, color: 'var(--fg1)',
                  transition: 'all 150ms',
                }}
              >
                {t}
              </button>
            ))}
          </div>
        </Field>

        <div style={{ height: 20 }} />

        <Field label="Default currency">
          <select
            value={currency}
            onChange={e => setCurrency(e.target.value)}
            style={{ padding: '12px 14px', borderRadius: 12, border: '1.5px solid var(--border-default)', fontSize: 15, width: '100%', outline: 'none' }}
          >
            <option>INR — Indian Rupee</option>
            <option>USD — US Dollar</option>
            <option>EUR — Euro</option>
            <option>GBP — British Pound</option>
          </select>
        </Field>

        <div style={{ height: 24 }} />

        <Field label="Members" hint="Add at least one other member.">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {memberInputs.map((m, i) => (
              <div key={i} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <Input
                  value={m.email}
                  onChange={e => updateMember(i, 'email', e.target.value)}
                  placeholder="email@example.com"
                  type="email"
                  style={{ flex: 2 }}
                />
                <Input
                  value={m.phone}
                  onChange={e => updateMember(i, 'phone', e.target.value)}
                  placeholder="Phone (optional)"
                  style={{ flex: 1 }}
                />
                {memberInputs.length > 1 && (
                  <button
                    onClick={() => removeMemberRow(i)}
                    style={{
                      width: 32, height: 32, borderRadius: 8, border: 'none',
                      background: 'var(--bg3)', cursor: 'pointer', flexShrink: 0,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}
                  >
                    <Icon name="close" size={14} color="var(--fg3)" />
                  </button>
                )}
              </div>
            ))}
            <button
              onClick={addMemberRow}
              style={{
                display: 'flex', alignItems: 'center', gap: 8,
                background: 'none', border: 'none', cursor: 'pointer',
                color: 'var(--sm-green-600)', fontWeight: 600, fontSize: 13, padding: '4px 0',
              }}
            >
              <Icon name="plus" size={14} color="var(--sm-green-600)" /> Add another member
            </button>
          </div>
        </Field>

        <div style={{ display: 'flex', gap: 10, marginTop: 24 }}>
          <Button variant="primary" size="lg" onClick={handleSubmit} disabled={loading}>
            {loading ? 'Creating…' : 'Create group →'}
          </Button>
          <Button variant="ghost" size="lg" onClick={() => navigate('/groups')}>
            Cancel
          </Button>
        </div>
      </Card>
    </AppShell>
  )
}
