import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import StatCard from '../components/ui/StatCard'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import Chip from '../components/ui/Chip'
import Icon from '../components/ui/Icon'
import Modal from '../components/ui/Modal'
import Field from '../components/ui/Field'
import Input from '../components/ui/Input'
import { getGroupDetails, getUserBalances, addMember } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import { extractMessage } from '../utils/messages'

function fmt(n) {
  const abs = Math.abs(Number(n) || 0).toFixed(2)
  const [w, f] = abs.split('.')
  return `₹${Number(w).toLocaleString()}.${f}`
}

export default function GroupDetail() {
  const navigate = useNavigate()
  const { id } = useParams()
  const groupName = decodeURIComponent(id)
  const { handleUnauthorized } = useAuth()

  const [tab, setTab] = useState('expenses')
  const [addMemberOpen, setAddMemberOpen] = useState(false)

  const [group, setGroup] = useState(null)
  const [balances, setBalances] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadData = async () => {
    setLoading(true)
    setError(null)
    const [detailsResult, balancesResult] = await Promise.all([
      getGroupDetails({ groupName }),
      getUserBalances({ groupName }),
    ])

    if (detailsResult.status === 401 || balancesResult.status === 401) {
      handleUnauthorized((detailsResult.status === 401 ? detailsResult : balancesResult).body)
      return
    }

    if (detailsResult.ok && detailsResult.body?.data) {
      setGroup(detailsResult.body.data)
    } else {
      setError(extractMessage(detailsResult.body, 'Failed to load group'))
    }

    if (balancesResult.ok && balancesResult.body?.data) {
      setBalances(balancesResult.body.data)
    }

    setLoading(false)
  }

  useEffect(() => {
    loadData()
  }, [groupName])

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

  if (loading) {
    return (
      <AppShell back={BackBtn} title={groupName} subtitle="Loading…">
        <div style={{ color: 'var(--fg3)', padding: 40, textAlign: 'center' }}>Loading group…</div>
      </AppShell>
    )
  }

  if (error || !group) {
    return (
      <AppShell back={BackBtn} title="Error">
        <div style={{
          background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)',
          padding: '12px 16px', borderRadius: 12, fontSize: 14,
        }}>
          {error || 'Group not found.'}
        </div>
      </AppShell>
    )
  }

  const members = group.members || []
  const totalToPay = parseFloat(balances?.totalToPay || '0')
  const totalToReceive = parseFloat(balances?.totalToReceive || '0')
  const netBalance = totalToReceive - totalToPay

  return (
    <AppShell
      back={BackBtn}
      title={<span><span style={{ marginRight: 12 }}>👥</span>{group.groupName}</span>}
      subtitle={`${group.memberCount || members.length} members · ${group.currency || ''}`}
      topRight={
        <div style={{ display: 'flex', gap: 10 }}>
          <Button variant="ghost" onClick={() => setAddMemberOpen(true)}>
            <Icon name="userPlus" size={16} /> Add member
          </Button>
          <Button variant="ghost" onClick={loadData} title="Refresh balances">
            <Icon name="refreshCw" size={16} />
          </Button>
          <Button variant="ghost" onClick={() => navigate('/settle', { state: { groupName: group.groupName, groupId: group.groupId, currency: group.currency } })}>
            Settle up
          </Button>
          <Button variant="primary" onClick={() => navigate('/add', { state: { groupId: group.groupId, groupName: group.groupName, currency: group.currency } })}>
            <Icon name="plus" size={16} /> Add expense
          </Button>
        </div>
      }
    >
      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16, marginBottom: 24 }}>
        <StatCard label="You are owed" value={fmt(totalToReceive)} accent="var(--sm-green-600)" />
        <StatCard label="You owe" value={fmt(totalToPay)} accent="var(--sm-danger)" />
        <StatCard
          label="Net balance"
          value={(netBalance >= 0 ? '+ ' : '− ') + fmt(Math.abs(netBalance))}
          accent={netBalance >= 0 ? 'var(--sm-green-600)' : 'var(--sm-danger)'}
        />
        <StatCard label="Currency" value={group.currency || '—'} />
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 20, borderBottom: '1px solid var(--border-subtle)' }}>
        {['expenses', 'balances', 'members', 'settings'].map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              padding: '12px 20px',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: 14,
              fontWeight: 600,
              textTransform: 'capitalize',
              color: tab === t ? 'var(--fg1)' : 'var(--fg3)',
              borderBottom: tab === t ? '2px solid var(--sm-green-500)' : '2px solid transparent',
              marginBottom: -1,
              transition: 'all 150ms',
            }}
          >
            {t}
          </button>
        ))}
      </div>

      {/* Expenses tab */}
      {tab === 'expenses' && (
        <Card style={{ padding: 60, textAlign: 'center' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>🧾</div>
          <div style={{ color: 'var(--fg3)', marginBottom: 16 }}>Expense history coming soon.</div>
          <Button variant="primary" onClick={() => navigate('/add', { state: { groupId: group.groupId, groupName: group.groupName, currency: group.currency } })}>
            <Icon name="plus" size={14} /> Add the first expense
          </Button>
        </Card>
      )}

      {/* Balances tab */}
      {tab === 'balances' && (
        <Card style={{ padding: 24 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 32 }}>
            <div>
              <h3 style={{ margin: '0 0 16px', fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--fg3)' }}>
                They owe you
              </h3>
              {(balances?.membersWhoNeedToPayUser || []).length === 0 && (
                <div style={{ color: 'var(--fg3)', fontSize: 13 }}>Nobody owes you in this group.</div>
              )}
              {(balances?.membersWhoNeedToPayUser || []).map((m, i) => (
                <div
                  key={m.email}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0',
                    borderBottom: '1px solid var(--border-subtle)',
                  }}
                >
                  <Avatar name={m.email.split('@')[0]} size={36} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>{m.email.split('@')[0]}</div>
                    <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{m.email}</div>
                  </div>
                  <div style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', color: 'var(--sm-green-600)' }}>
                    + {fmt(m.amount)}
                  </div>
                </div>
              ))}
            </div>
            <div>
              <h3 style={{ margin: '0 0 16px', fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--fg3)' }}>
                You owe them
              </h3>
              {(balances?.membersUserNeedsToPay || []).length === 0 && (
                <div style={{ color: 'var(--fg3)', fontSize: 13 }}>You don't owe anyone in this group.</div>
              )}
              {(balances?.membersUserNeedsToPay || []).map((m, i) => (
                <div
                  key={m.email}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0',
                    borderBottom: '1px solid var(--border-subtle)',
                  }}
                >
                  <Avatar name={m.email.split('@')[0]} size={36} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>{m.email.split('@')[0]}</div>
                    <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{m.email}</div>
                  </div>
                  <div style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', color: 'var(--sm-danger)' }}>
                    − {fmt(m.amount)}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Card>
      )}

      {/* Members tab */}
      {tab === 'members' && (
        <Card style={{ padding: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>{members.length} members</h3>
            <Button variant="primary" size="sm" onClick={() => setAddMemberOpen(true)}>
              <Icon name="userPlus" size={14} /> Add member
            </Button>
          </div>
          {members.map((m, i) => (
            <div
              key={m.email}
              style={{
                display: 'flex', alignItems: 'center', gap: 14, padding: '14px 0',
                borderBottom: '1px solid var(--border-subtle)',
              }}
            >
              <Avatar name={m.email.split('@')[0]} size={44} />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600 }}>{m.email.split('@')[0]}</div>
                <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{m.email}</div>
              </div>
              {i === 0 && <Chip variant="brand">Admin</Chip>}
            </div>
          ))}
        </Card>
      )}

      {/* Settings tab */}
      {tab === 'settings' && (
        <Card style={{ padding: 32, maxWidth: 600 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <Field label="Group name"><Input defaultValue={group.groupName} /></Field>
            <Field label="Currency"><Input defaultValue={group.currency} style={{ width: 120 }} /></Field>
            <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
              <Button variant="primary">Save changes</Button>
              <Button variant="ghost">Cancel</Button>
            </div>
            <div style={{ height: 1, background: 'var(--border-subtle)', margin: '12px 0' }} />
            <div>
              <h4 style={{ margin: '0 0 8px', color: 'var(--sm-danger)' }}>Danger zone</h4>
              <div style={{ display: 'flex', gap: 10 }}>
                <Button variant="ghost">Archive group</Button>
                <Button variant="danger"><Icon name="trash" size={14} /> Delete group</Button>
              </div>
            </div>
          </div>
        </Card>
      )}

      <AddMemberModal
        open={addMemberOpen}
        onClose={() => setAddMemberOpen(false)}
        groupName={group.groupName}
        onSuccess={loadData}
      />
    </AppShell>
  )
}

function AddMemberModal({ open, onClose, groupName, onSuccess }) {
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const { handleUnauthorized } = useAuth()

  const handleSubmit = async () => {
    if (!email.trim()) return
    setLoading(true)
    setError(null)
    const result = await addMember({ groupName, memberEmail: email.trim(), memberPhone: phone.trim() || null })
    setLoading(false)
    if (result.status === 401) { handleUnauthorized(result.body); return }
    if (result.ok) {
      setEmail('')
      setPhone('')
      onClose()
      onSuccess()
    } else {
      setError(extractMessage(result.body, 'Failed to add member'))
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={`Add member to ${groupName}`}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {error && (
          <div style={{ background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)', padding: '10px 14px', borderRadius: 10, fontSize: 13 }}>
            {error}
          </div>
        )}
        <Field label="Email address">
          <Input
            value={email}
            onChange={e => setEmail(e.target.value)}
            placeholder="alex@example.com"
            type="email"
          />
        </Field>
        <Field label="Phone (optional)">
          <Input
            value={phone}
            onChange={e => setPhone(e.target.value)}
            placeholder="+91 98765 43210"
          />
        </Field>
        <Button variant="primary" size="lg" onClick={handleSubmit} disabled={loading} style={{ marginTop: 8 }}>
          {loading ? 'Adding…' : 'Add member'}
        </Button>
      </div>
    </Modal>
  )
}
