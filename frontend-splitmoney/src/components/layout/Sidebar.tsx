import { useNavigate, useLocation } from 'react-router-dom'
import Avatar from '../ui/Avatar'
import Button from '../ui/Button'
import Icon from '../ui/Icon'
import { useAuth } from '../../context/AuthContext'
import { ME } from '../../data'

const NAV_ITEMS = [
  { path: '/dashboard', label: 'Dashboard', icon: 'home' },
  { path: '/groups',    label: 'Groups',    icon: 'users' },
  { path: '/friends',   label: 'Friends',   icon: 'user' },
  { path: '/activity',  label: 'Activity',  icon: 'bell' },
  { path: '/analytics', label: 'Analytics', icon: 'chart' },
]

const BOTTOM_ITEMS = [
  { path: '/settings', label: 'Settings', icon: 'settings' },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { logout, session } = useAuth()

  const isActive = (path) =>
    pathname === path || (path === '/groups' && pathname.startsWith('/groups'))

  return (
    <aside
      style={{
        width: 248,
        background: 'white',
        borderRight: '1px solid var(--border-subtle)',
        padding: '20px 14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 4,
        position: 'sticky',
        top: 0,
        height: '100vh',
        overflowY: 'auto',
      }}
    >
      {/* Logo */}
      <button
        onClick={() => navigate('/dashboard')}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          padding: '6px 10px 18px',
          textAlign: 'left',
          display: 'flex',
          alignItems: 'center',
          gap: 10,
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            background: 'var(--grad-brand)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 800,
            fontSize: 18,
            color: '#03241A',
          }}
        >
          ₹
        </div>
        <span
          style={{
            fontFamily: 'var(--font-display)',
            fontWeight: 800,
            fontSize: 20,
            color: 'var(--sm-ink-950)',
            letterSpacing: '-0.02em',
          }}
        >
          SplitMoney
        </span>
      </button>

      {/* Add Expense CTA */}
      <Button
        variant="primary"
        onClick={() => navigate('/add')}
        style={{ marginBottom: 10, width: '100%' }}
      >
        <Icon name="plus" size={16} />
        Add expense
      </Button>

      {/* Main Nav */}
      {NAV_ITEMS.map(item => (
        <NavLink
          key={item.path}
          item={item}
          active={isActive(item.path)}
          onClick={() => navigate(item.path)}
        />
      ))}

      <div style={{ flex: 1 }} />

      {/* Upgrade card */}
      <div
        style={{
          padding: 12,
          borderRadius: 16,
          background: 'var(--sm-ink-950)',
          color: 'white',
          position: 'relative',
          overflow: 'hidden',
          marginBottom: 8,
        }}
      >
        <div style={{ position: 'absolute', inset: '-40%', background: 'var(--grad-glow)' }} />
        <div style={{ position: 'relative' }}>
          <div style={{ fontSize: 11, opacity: 0.65, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Upgrade
          </div>
          <div style={{ fontWeight: 700, fontSize: 15, marginTop: 2 }}>Go Plus — ₹4/mo</div>
          <div style={{ fontSize: 12, opacity: 0.7, marginTop: 2 }}>
            Unlimited groups, OCR, multi-currency.
          </div>
          <Button variant="lime" size="sm" style={{ marginTop: 10, width: '100%' }}>
            Upgrade
          </Button>
        </div>
      </div>

      {/* Bottom Nav */}
      {BOTTOM_ITEMS.map(item => (
        <NavLink
          key={item.path}
          item={item}
          active={isActive(item.path)}
          onClick={() => navigate(item.path)}
        />
      ))}

      {/* User profile + logout */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6 }}>
        <button
          onClick={() => navigate('/profile')}
          style={{
            display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px',
            borderRadius: 12, background: 'var(--bg3)', border: 'none',
            cursor: 'pointer', flex: 1, textAlign: 'left', minWidth: 0,
          }}
        >
          <Avatar name={session?.email || ME.name} size={32} bg={ME.bg} />
          <div style={{ minWidth: 0, flex: 1 }}>
            <div style={{ fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', color: 'var(--fg1)' }}>
              {session?.email?.split('@')[0] || ME.name}
            </div>
            <div style={{ fontSize: 11, color: 'var(--fg3)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {session?.email || ME.email}
            </div>
          </div>
        </button>
        <button
          onClick={logout}
          title="Sign out"
          style={{
            width: 36, height: 36, borderRadius: 10, border: '1.5px solid var(--border-default)',
            background: 'white', cursor: 'pointer', display: 'flex',
            alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}
        >
          <Icon name="logout" size={16} color="var(--fg3)" />
        </button>
      </div>
    </aside>
  )
}

function NavLink({ item, active, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '10px 14px',
        borderRadius: 12,
        border: 'none',
        cursor: 'pointer',
        color: active ? 'var(--sm-green-950)' : 'var(--fg2)',
        background: active ? 'var(--sm-green-50)' : 'transparent',
        fontWeight: active ? 600 : 500,
        fontSize: 14,
        width: '100%',
        textAlign: 'left',
        transition: 'all 150ms var(--ease-out)',
      }}
    >
      <Icon name={item.icon} size={18} color={active ? 'var(--sm-green-600)' : 'var(--fg3)'} />
      {item.label}
    </button>
  )
}
