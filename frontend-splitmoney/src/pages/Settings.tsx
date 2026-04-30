import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/layout/AppShell'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Icon from '../components/ui/Icon'

function Toggle({ on, onChange }) {
  return (
    <button
      onClick={() => onChange(!on)}
      style={{
        width: 44, height: 26, borderRadius: 999, border: 'none', cursor: 'pointer',
        background: on ? 'var(--sm-green-500)' : 'var(--bg4)', position: 'relative',
        transition: 'background 200ms',
        flexShrink: 0,
      }}
    >
      <div
        style={{
          position: 'absolute', top: 3,
          left: on ? 21 : 3,
          width: 20, height: 20, borderRadius: 999, background: 'white',
          boxShadow: '0 2px 4px rgba(0,0,0,0.2)', transition: 'left 200ms',
        }}
      />
    </button>
  )
}

function SettingGroup({ title, items }) {
  const [state, setState] = useState(items.map(i => i.on))
  return (
    <Card style={{ padding: 0 }}>
      <div style={{ padding: '18px 24px', borderBottom: '1px solid var(--border-subtle)' }}>
        <h3 style={{ margin: 0, fontSize: 17, fontWeight: 600 }}>{title}</h3>
      </div>
      {items.map((item, i) => (
        <div
          key={i}
          style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '16px 24px',
            borderTop: i === 0 ? 'none' : '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ fontSize: 14, paddingRight: 20 }}>{item.label}</div>
          <Toggle on={state[i]} onChange={v => setState(state.map((s, j) => j === i ? v : s))} />
        </div>
      ))}
    </Card>
  )
}

export default function Settings() {
  const navigate = useNavigate()

  return (
    <AppShell title="Settings" subtitle="Preferences, notifications, privacy.">
      <div style={{ maxWidth: 800, display: 'flex', flexDirection: 'column', gap: 20 }}>
        <SettingGroup
          title="Notifications"
          items={[
            { label: 'Email me when a friend adds an expense', on: true },
            { label: 'Email me when someone pays me', on: true },
            { label: 'Weekly "who owes who" digest', on: true },
            { label: 'Promotional emails', on: false },
          ]}
        />
        <SettingGroup
          title="Privacy"
          items={[
            { label: 'Show my profile to people I share groups with', on: true },
            { label: 'Let friends invite me by email', on: true },
            { label: 'Share anonymous analytics', on: false },
          ]}
        />
        <SettingGroup
          title="Appearance"
          items={[
            { label: 'Use dark mode', on: false },
            { label: 'Show running balances on dashboard', on: true },
            { label: 'Tabular numerals in amounts', on: true },
          ]}
        />

        {/* Security */}
        <Card style={{ padding: 28 }}>
          <h3 style={{ margin: '0 0 16px', fontSize: 17, fontWeight: 600 }}>Security</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <Button variant="ghost" style={{ justifyContent: 'flex-start' }}>Change password</Button>
            <Button variant="ghost" style={{ justifyContent: 'flex-start' }}>Set up two-factor authentication</Button>
            <Button variant="ghost" style={{ justifyContent: 'flex-start' }}>Active sessions · 2 devices</Button>
          </div>
        </Card>

        {/* Danger zone */}
        <Card style={{ padding: 28 }}>
          <h3 style={{ margin: '0 0 4px', fontSize: 17, fontWeight: 600, color: 'var(--sm-danger)' }}>
            Danger zone
          </h3>
          <p style={{ color: 'var(--fg3)', fontSize: 13, marginTop: 0 }}>
            These are permanent. Export your data first if needed.
          </p>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button variant="ghost">Export all data (CSV)</Button>
            <Button
              variant="ghost"
              onClick={() => navigate('/signin')}
            >
              <Icon name="logout" size={14} /> Sign out everywhere
            </Button>
            <Button variant="danger">
              <Icon name="trash" size={14} /> Delete account
            </Button>
          </div>
        </Card>
      </div>
    </AppShell>
  )
}
