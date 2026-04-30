import { useState } from 'react'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Chip from '../components/ui/Chip'
import Avatar from '../components/ui/Avatar'
import { ACTIVITY, PEOPLE, fmt } from '../data'

export default function Activity() {
  const [filter, setFilter] = useState('all')
  const items = ACTIVITY.filter(a => filter === 'all' ? true : a.type === filter)

  return (
    <AppShell title="Activity" subtitle="Everything that happened across your groups.">
      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        {[['all', 'All'], ['expense', 'Expenses'], ['payment', 'Payments'], ['group', 'Groups']].map(([k, l]) => (
          <Chip key={k} variant={filter === k ? 'dark' : 'default'} onClick={() => setFilter(k)}>
            {l}
          </Chip>
        ))}
      </div>

      <Card style={{ padding: 0 }}>
        {items.map((a, i) => {
          const who = PEOPLE[a.who]
          return (
            <div
              key={a.id}
              style={{
                display: 'flex', gap: 14, padding: '18px 24px',
                borderTop: i === 0 ? 'none' : '1px solid var(--border-subtle)',
                alignItems: 'center',
                transition: 'background 150ms',
              }}
              onMouseEnter={e => e.currentTarget.style.background = 'var(--bg3)'}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
            >
              <Avatar name={who.name} bg={who.bg} size={40} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14.5, lineHeight: 1.4 }}>
                  <b>{a.who === 'me' ? 'You' : who.name.split(' ')[0]}</b> {a.title}
                  {a.amount != null && (
                    <> for <span style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>{fmt(a.amount)}</span></>
                  )}
                </div>
                <div style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 3 }}>
                  {a.group} · {a.time}
                </div>
              </div>
              {a.yourShare != null && (
                <div
                  style={{
                    fontWeight: 700, fontVariantNumeric: 'tabular-nums',
                    color: a.yourShare > 0 ? 'var(--sm-green-600)' : 'var(--sm-danger)',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {a.yourShare > 0 ? '+' : '−'} {fmt(Math.abs(a.yourShare))}
                </div>
              )}
            </div>
          )
        })}
        {items.length === 0 && (
          <div style={{ padding: 60, textAlign: 'center', color: 'var(--fg3)' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>📭</div>
            No activity yet.
          </div>
        )}
      </Card>
    </AppShell>
  )
}
