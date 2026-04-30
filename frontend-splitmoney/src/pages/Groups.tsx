import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Chip from '../components/ui/Chip'
import Icon from '../components/ui/Icon'
import { AvatarStack } from '../components/ui/Avatar'
import { getUserGroups } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'

function transformGroup(g) {
  return {
    id: String(g.groupId),
    groupId: g.groupId,
    groupName: g.groupName,
    name: g.groupName,
    description: g.groupDescription || '',
    currency: g.currency || 'INR',
    createdBy: g.createdBy || '',
    createdDate: g.createdDate || '',
    emoji: '👥',
    color: 'linear-gradient(135deg, var(--sm-green-50), var(--sm-green-100))',
    members: [],
    youOwe: 0,
    youAreOwed: 0,
    total: 0,
    settled: false,
    lastActivity: g.createdDate ? new Date(g.createdDate).toLocaleDateString() : '',
  }
}

export default function Groups() {
  const navigate = useNavigate()
  const { handleUnauthorized } = useAuth()
  const [filter, setFilter] = useState('all')
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError(null)
      const result = await getUserGroups()
      if (cancelled) return
      if (result.status === 401) {
        handleUnauthorized(result.body)
        return
      }
      if (result.ok && result.body?.data?.groups) {
        setGroups(result.body.data.groups.map(transformGroup))
      } else {
        setError(result.body?.responseMessage || 'Failed to load groups')
      }
      setLoading(false)
    }
    load()
    return () => { cancelled = true }
  }, [handleUnauthorized])

  const filtered = groups.filter(g =>
    filter === 'all' ? true : filter === 'active' ? !g.settled : g.settled
  )

  if (loading) {
    return (
      <AppShell title="Groups" subtitle="Loading…">
        <div style={{ color: 'var(--fg3)', padding: 40, textAlign: 'center' }}>Loading groups…</div>
      </AppShell>
    )
  }

  return (
    <AppShell
      title="Groups"
      subtitle={`${groups.length} total · ${groups.filter(g => !g.settled).length} active`}
      topRight={
        <div style={{ display: 'flex', gap: 10 }}>
          <Button variant="ghost" onClick={() => navigate('/groups/new')}>
            <Icon name="plus" size={16} /> New group
          </Button>
          <Button variant="primary" onClick={() => navigate('/add')}>
            <Icon name="plus" size={16} /> Add expense
          </Button>
        </div>
      }
    >
      {error && (
        <div style={{
          background: 'var(--sm-danger-50, #FFF0F0)', color: 'var(--sm-danger)',
          padding: '12px 16px', borderRadius: 12, marginBottom: 16, fontSize: 14,
        }}>
          {error}
        </div>
      )}

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        {[['all', 'All'], ['active', 'Active'], ['settled', 'Settled']].map(([k, l]) => (
          <Chip key={k} variant={filter === k ? 'dark' : 'default'} onClick={() => setFilter(k)}>
            {l}
          </Chip>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
        {filtered.map(g => (
          <Card
            key={g.id}
            onClick={() => navigate('/groups/' + encodeURIComponent(g.groupName))}
            style={{ padding: 0, overflow: 'hidden' }}
          >
            <div
              style={{
                height: 110,
                background: g.color,
                position: 'relative',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 56,
              }}
            >
              {g.emoji}
              {g.settled && (
                <div style={{ position: 'absolute', top: 12, right: 12 }}>
                  <Chip variant="settled">✓ Settled</Chip>
                </div>
              )}
            </div>
            <div style={{ padding: 20 }}>
              <div style={{ fontWeight: 600, fontSize: 17, letterSpacing: '-0.01em' }}>{g.name}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
                <span style={{ fontSize: 12, color: 'var(--fg3)' }}>
                  {g.currency} · Created by {g.createdBy.split('@')[0]}
                </span>
              </div>
              <div style={{ height: 1, background: 'var(--border-subtle)', margin: '16px 0' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--fg3)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
                    Created
                  </div>
                  <div style={{ fontWeight: 600, fontSize: 14, color: 'var(--fg2)' }}>
                    {g.lastActivity}
                  </div>
                </div>
                <Chip variant="default">{g.currency}</Chip>
              </div>
            </div>
          </Card>
        ))}

        {/* New group card */}
        <Card
          onClick={() => navigate('/groups/new')}
          style={{
            padding: 20,
            border: '1.5px dashed var(--border-default)',
            background: 'transparent',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 280,
            color: 'var(--fg2)',
            boxShadow: 'none',
          }}
        >
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: 999,
              background: 'var(--sm-green-50)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Icon name="plus" size={28} color="var(--sm-green-600)" />
          </div>
          <div style={{ fontWeight: 600, marginTop: 14 }}>New group</div>
          <div style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 4 }}>Trip, home, couple…</div>
        </Card>
      </div>
    </AppShell>
  )
}
