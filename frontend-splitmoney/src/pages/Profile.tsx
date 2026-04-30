import { useState } from 'react'
import AppShell from '../components/layout/AppShell'
import StatCard from '../components/ui/StatCard'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import Chip from '../components/ui/Chip'
import Field from '../components/ui/Field'
import Input from '../components/ui/Input'
import { ME } from '../data'

export default function Profile() {
  const [tab, setTab] = useState('overview')

  return (
    <AppShell title="Your profile" subtitle="How you appear to friends and groups.">
      <div style={{ maxWidth: 900 }}>
        {/* Profile header */}
        <Card style={{ padding: 0, overflow: 'hidden', marginBottom: 24 }}>
          <div style={{ height: 140, background: 'var(--grad-mint)', position: 'relative' }}>
            <div style={{ position: 'absolute', inset: '-20%', background: 'var(--grad-glow)', opacity: 0.5 }} />
          </div>
          <div
            style={{
              padding: '0 32px 28px',
              display: 'flex', alignItems: 'flex-end', gap: 24,
              marginTop: -56,
            }}
          >
            <div style={{ padding: 6, background: 'white', borderRadius: 999, boxShadow: 'var(--shadow-md)' }}>
              <Avatar name={ME.name} bg={ME.bg} size={104} />
            </div>
            <div style={{ flex: 1, paddingBottom: 8 }}>
              <h2
                style={{
                  margin: 0, fontFamily: 'var(--font-display)', fontSize: 32,
                  fontWeight: 800, letterSpacing: '-0.02em',
                }}
              >
                {ME.name}
              </h2>
              <div style={{ color: 'var(--fg3)', fontSize: 14 }}>
                {ME.email} · Member since Jan 2024
              </div>
            </div>
            <Button variant="ghost">Edit profile</Button>
          </div>
        </Card>

        {/* Stats */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
          <StatCard label="Net balance"    value="+₹85.90"  accent="var(--sm-green-600)" />
          <StatCard label="Active groups"  value="3" />
          <StatCard label="Friends"        value="7" />
          <StatCard label="Total expenses" value="148" />
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: 4, marginBottom: 20, borderBottom: '1px solid var(--border-subtle)' }}>
          {[['overview', 'Overview'], ['payment', 'Payment methods'], ['linked', 'Linked accounts']].map(([t, l]) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              style={{
                padding: '12px 20px', background: 'none', border: 'none', cursor: 'pointer',
                fontSize: 14, fontWeight: 600,
                color: tab === t ? 'var(--fg1)' : 'var(--fg3)',
                borderBottom: tab === t ? '2px solid var(--sm-green-500)' : '2px solid transparent',
                marginBottom: -1, transition: 'all 150ms',
              }}
            >
              {l}
            </button>
          ))}
        </div>

        {/* Overview tab */}
        {tab === 'overview' && (
          <Card style={{ padding: 32 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <Field label="Full name"><Input defaultValue={ME.name} /></Field>
              <Field label="Display name" hint="What friends see."><Input defaultValue="Riley" /></Field>
              <Field label="Email"><Input defaultValue={ME.email} /></Field>
              <Field label="Phone"><Input defaultValue="+1 (415) 555-0142" /></Field>
              <Field label="Default currency">
                <select style={{ padding: '12px 14px', borderRadius: 12, border: '1.5px solid var(--border-default)', fontSize: 15, width: '100%', outline: 'none' }}>
                  <option>INR — Indian Rupee</option>
                  <option>EUR — Euro</option>
                  <option>GBP — British Pound</option>
                </select>
              </Field>
              <Field label="Timezone">
                <select style={{ padding: '12px 14px', borderRadius: 12, border: '1.5px solid var(--border-default)', fontSize: 15, width: '100%', outline: 'none' }}>
                  <option>Pacific Time (San Francisco)</option>
                  <option>Eastern Time (New York)</option>
                </select>
              </Field>
            </div>
            <div style={{ display: 'flex', gap: 10, marginTop: 24 }}>
              <Button variant="primary">Save changes</Button>
              <Button variant="ghost">Cancel</Button>
            </div>
          </Card>
        )}

        {/* Payment methods */}
        {tab === 'payment' && (
          <Card style={{ padding: 0 }}>
            {[
              { k: 'venmo',   label: 'Venmo',       sub: '@riley-chen',      color: '#3D95CE', connected: true  },
              { k: 'cashapp', label: 'Cash App',     sub: '$riley',           color: '#00D54B', connected: true  },
              { k: 'paypal',  label: 'PayPal',       sub: 'riley@gmail.com',  color: '#003087', connected: false },
              { k: 'card',    label: 'Credit card',  sub: 'Visa ending 4242', color: '#1A1F71', connected: true  },
            ].map((m, i) => (
              <div
                key={m.k}
                style={{
                  display: 'flex', alignItems: 'center', gap: 14, padding: '20px 24px',
                  borderTop: i === 0 ? 'none' : '1px solid var(--border-subtle)',
                }}
              >
                <div
                  style={{
                    width: 44, height: 44, borderRadius: 12, background: m.color, color: 'white',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 18,
                    flexShrink: 0,
                  }}
                >
                  {m.label[0]}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600 }}>{m.label}</div>
                  <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{m.sub}</div>
                </div>
                {m.connected ? (
                  <>
                    <Chip variant="owed">✓ Connected</Chip>
                    <Button variant="ghost" size="sm">Disconnect</Button>
                  </>
                ) : (
                  <Button variant="primary" size="sm">Connect</Button>
                )}
              </div>
            ))}
          </Card>
        )}

        {/* Linked accounts */}
        {tab === 'linked' && (
          <Card style={{ padding: 32 }}>
            <div style={{ color: 'var(--fg3)', fontSize: 14, marginBottom: 16 }}>
              Sign in faster by connecting social accounts.
            </div>
            {[
              'Google — riley@gmail.com',
              'Apple — not connected',
              'GitHub — @rileyc',
            ].map((l, i) => (
              <div
                key={i}
                style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  padding: '14px 0', borderBottom: '1px solid var(--border-subtle)',
                }}
              >
                <div style={{ fontSize: 14 }}>{l}</div>
                <Button variant="ghost" size="sm">
                  {l.includes('not') ? 'Connect' : 'Disconnect'}
                </Button>
              </div>
            ))}
          </Card>
        )}
      </div>
    </AppShell>
  )
}
