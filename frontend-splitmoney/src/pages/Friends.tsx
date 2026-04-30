import { useNavigate } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import Icon from '../components/ui/Icon'
import { FRIENDS, PEOPLE, fmt } from '../data'

function FriendRow({ p, f, onSettle }) {
  return (
    <div
      style={{
        display: 'flex', alignItems: 'center', gap: 14, padding: '16px 24px',
        borderTop: '1px solid var(--border-subtle)',
      }}
    >
      <Avatar name={p.name} bg={p.bg} size={44} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 600 }}>{p.name}</div>
        <div style={{ fontSize: 12, color: 'var(--fg3)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {f.shared.join(' · ')}
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div
          style={{
            fontWeight: 700, fontVariantNumeric: 'tabular-nums',
            color: f.balance > 0 ? 'var(--sm-green-600)' : 'var(--sm-danger)',
          }}
        >
          {f.balance > 0 ? '+' : '−'} {fmt(Math.abs(f.balance))}
        </div>
        {f.balance < 0 && (
          <button
            onClick={onSettle}
            style={{
              marginTop: 4, border: 'none', background: 'var(--sm-green-500)', color: '#03241A',
              padding: '4px 10px', borderRadius: 999, fontSize: 11, fontWeight: 700, cursor: 'pointer',
            }}
          >
            Pay now
          </button>
        )}
        {f.balance > 0 && (
          <button
            style={{
              marginTop: 4, border: '1px solid var(--border-default)', background: 'white', color: 'var(--fg2)',
              padding: '4px 10px', borderRadius: 999, fontSize: 11, fontWeight: 600, cursor: 'pointer',
            }}
          >
            Remind
          </button>
        )}
      </div>
    </div>
  )
}

export default function Friends() {
  const navigate = useNavigate()
  const owed = FRIENDS.filter(f => f.balance > 0).reduce((s, f) => s + f.balance, 0)
  const owes = FRIENDS.filter(f => f.balance < 0).reduce((s, f) => s - f.balance, 0)

  return (
    <AppShell
      title="Friends"
      subtitle={`${FRIENDS.length} friends · ${fmt(owed - owes, true)} net balance`}
      topRight={
        <Button variant="primary">
          <Icon name="userPlus" size={16} /> Add friend
        </Button>
      }
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        {/* Owes you */}
        <Card style={{ padding: 0 }}>
          <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-subtle)' }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600, color: 'var(--sm-green-600)' }}>
              Owes you · {fmt(owed)}
            </h3>
          </div>
          {FRIENDS.filter(f => f.balance > 0).map(f => (
            <FriendRow key={f.id} p={PEOPLE[f.id]} f={f} onSettle={() => navigate('/settle')} />
          ))}
        </Card>

        {/* You owe */}
        <Card style={{ padding: 0 }}>
          <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-subtle)' }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600, color: 'var(--sm-danger)' }}>
              You owe · {fmt(owes)}
            </h3>
          </div>
          {FRIENDS.filter(f => f.balance < 0).map(f => (
            <FriendRow key={f.id} p={PEOPLE[f.id]} f={f} onSettle={() => navigate('/settle')} />
          ))}
        </Card>
      </div>
    </AppShell>
  )
}
