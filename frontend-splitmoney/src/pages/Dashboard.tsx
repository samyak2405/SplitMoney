import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import StatCard from '../components/ui/StatCard'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Chip from '../components/ui/Chip'
import Icon from '../components/ui/Icon'
import { getUserGroups } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'

export default function Dashboard() {
  const navigate = useNavigate()
  const { session, handleUnauthorized } = useAuth()

  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)

  const displayName = session?.email?.split('@')[0] || 'there'

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      const result = await getUserGroups()
      if (cancelled) return
      if (result.status === 401) { handleUnauthorized(result.body); return }
      if (result.ok && result.body?.data?.groups) {
        setGroups(result.body.data.groups)
      }
      setLoading(false)
    }
    load()
    return () => { cancelled = true }
  }, [handleUnauthorized])

  const activeGroups = groups.filter(g => !g.settled)

  return (
    <AppShell
      title={`Hey, ${displayName} 👋`}
      subtitle={`${activeGroups.length} active group${activeGroups.length !== 1 ? 's' : ''}`}
      topRight={
        <Button variant="primary" onClick={() => navigate('/add')}>
          <Icon name="plus" size={16} /> Add expense
        </Button>
      }
    >
      {/* Stat cards */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr 1fr', gap: 16, marginBottom: 24 }}>
        <StatCard
          dark
          label="Your overall balance"
          value="—"
          accent="#D8FF5E"
          sub="Open a group to see your balance"
        >
          <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
            <Button variant="lime" size="sm" onClick={() => navigate('/settle')}>
              Settle up
            </Button>
          </div>
        </StatCard>
        <StatCard label="Active groups" value={String(activeGroups.length)} accent="var(--sm-green-600)" sub="Tap to view details" />
        <StatCard label="Total groups" value={String(groups.length)} accent="var(--sm-green-600)" sub="All groups you're in" />
      </div>

      {/* Groups + Quick actions */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 24 }}>
        {/* Groups */}
        <Card style={{ padding: 0 }}>
          <div
            style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              padding: '20px 24px',
            }}
          >
            <div>
              <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>Your groups</h2>
              <div style={{ fontSize: 13, color: 'var(--fg3)' }}>{activeGroups.length} active</div>
            </div>
            <button
              onClick={() => navigate('/groups')}
              style={{ color: 'var(--sm-green-600)', fontSize: 13, fontWeight: 600, background: 'none', border: 'none', cursor: 'pointer' }}
            >
              View all →
            </button>
          </div>

          {loading && (
            <div style={{ padding: '20px 24px', color: 'var(--fg3)', fontSize: 13 }}>Loading groups…</div>
          )}

          {!loading && groups.length === 0 && (
            <div style={{ padding: '32px 24px', textAlign: 'center', color: 'var(--fg3)' }}>
              <div style={{ fontSize: 36, marginBottom: 8 }}>👥</div>
              <div style={{ marginBottom: 12 }}>No groups yet</div>
              <Button variant="primary" size="sm" onClick={() => navigate('/groups/new')}>
                Create your first group
              </Button>
            </div>
          )}

          <div>
            {activeGroups.slice(0, 6).map(g => (
              <div
                key={g.groupId}
                onClick={() => navigate('/groups/' + encodeURIComponent(g.groupName))}
                style={{
                  display: 'flex', alignItems: 'center', gap: 16, padding: '16px 24px',
                  borderTop: '1px solid var(--border-subtle)', cursor: 'pointer', transition: 'background 150ms',
                }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg3)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                <div
                  style={{
                    width: 48, height: 48, borderRadius: 14,
                    background: 'linear-gradient(135deg, var(--sm-green-50), var(--sm-green-100))',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 24, flexShrink: 0,
                  }}
                >
                  👥
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>{g.groupName}</div>
                  <div style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 2 }}>
                    {g.currency} · by {g.createdBy?.split('@')[0]}
                  </div>
                </div>
                <Chip variant="default">{g.currency}</Chip>
              </div>
            ))}
          </div>
        </Card>

        {/* Quick actions */}
        <Card style={{ padding: 24 }}>
          <h2 style={{ margin: '0 0 20px', fontSize: 20, fontWeight: 600 }}>Quick actions</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Button variant="primary" size="lg" style={{ width: '100%', justifyContent: 'flex-start', gap: 12 }} onClick={() => navigate('/add')}>
              <Icon name="plus" size={18} /> Add an expense
            </Button>
            <Button variant="ghost" size="lg" style={{ width: '100%', justifyContent: 'flex-start', gap: 12 }} onClick={() => navigate('/groups/new')}>
              <Icon name="users" size={18} /> Create a group
            </Button>
            <Button variant="ghost" size="lg" style={{ width: '100%', justifyContent: 'flex-start', gap: 12 }} onClick={() => navigate('/settle')}>
              <Icon name="check" size={18} /> Settle up
            </Button>
          </div>

          <div style={{ height: 1, background: 'var(--border-subtle)', margin: '20px 0' }} />

          <div style={{ fontSize: 13, color: 'var(--fg3)' }}>
            <div style={{ fontWeight: 600, color: 'var(--fg2)', marginBottom: 8 }}>Signed in as</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>{session?.email}</div>
          </div>
        </Card>
      </div>
    </AppShell>
  )
}
