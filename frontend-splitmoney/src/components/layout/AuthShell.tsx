import { useNavigate } from 'react-router-dom'
import Chip from '../ui/Chip'

export function AuthShell({ children }) {
  const navigate = useNavigate()
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        background: 'var(--sm-paper)',
      }}
    >
      <div
        style={{
          padding: '32px 40px',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <button
          onClick={() => navigate('/')}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 10,
            padding: 0,
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
        <div
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '32px 0',
          }}
        >
          <div style={{ width: '100%', maxWidth: 420 }}>{children}</div>
        </div>
        <div style={{ fontSize: 12, color: 'var(--fg3)' }}>© 2026 SplitMoney, Inc.</div>
      </div>
      <AuthSide />
    </div>
  )
}

function MiniCard({ label, value, accent, emoji, title, sub }) {
  if (label) {
    return (
      <div
        style={{
          background: 'rgba(255,255,255,0.06)',
          border: '1px solid rgba(255,255,255,0.12)',
          borderRadius: 18,
          padding: 16,
          backdropFilter: 'blur(10px)',
        }}
      >
        <div
          style={{
            fontSize: 11,
            textTransform: 'uppercase',
            letterSpacing: '0.1em',
            opacity: 0.6,
            color: 'white',
          }}
        >
          {label}
        </div>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontWeight: 800,
            fontSize: 32,
            letterSpacing: '-0.02em',
            color: accent,
            fontVariantNumeric: 'tabular-nums',
            marginTop: 4,
          }}
        >
          {value}
        </div>
      </div>
    )
  }
  return (
    <div
      style={{
        background: 'rgba(255,255,255,0.06)',
        border: '1px solid rgba(255,255,255,0.12)',
        borderRadius: 18,
        padding: 16,
        backdropFilter: 'blur(10px)',
        display: 'flex',
        gap: 10,
        alignItems: 'center',
      }}
    >
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: 12,
          background: 'rgba(255,255,255,0.08)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 20,
          flexShrink: 0,
        }}
      >
        {emoji}
      </div>
      <div style={{ minWidth: 0 }}>
        <div
          style={{
            fontSize: 13,
            fontWeight: 600,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            color: 'white',
          }}
        >
          {title}
        </div>
        <div
          style={{
            fontSize: 11,
            opacity: 0.6,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            color: 'white',
          }}
        >
          {sub}
        </div>
      </div>
    </div>
  )
}

export function AuthSide() {
  return (
    <div
      style={{
        background: 'var(--sm-ink-950)',
        color: 'white',
        position: 'relative',
        overflow: 'hidden',
        padding: 48,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      <div style={{ position: 'absolute', inset: '-20%', background: 'var(--grad-glow)' }} />
      <div style={{ position: 'relative' }}>
        <Chip
          style={{ background: 'rgba(168,232,20,0.15)', color: '#D8FF5E', border: 'none' }}
        >
          ✨ Trusted by 200k+ groups
        </Chip>
      </div>
      <div style={{ position: 'relative' }}>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontWeight: 800,
            fontSize: 52,
            letterSpacing: '-0.03em',
            lineHeight: 1.05,
          }}
        >
          The friendliest
          <br />
          way to{' '}
          <span
            style={{
              background: 'linear-gradient(135deg, #A8E814, #35C974)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              color: 'transparent',
            }}
          >
            split the bill.
          </span>
        </div>
        <div
          style={{
            marginTop: 36,
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: 12,
          }}
        >
          <MiniCard label="Your balance" value="+₹85.90" accent="#D8FF5E" />
          <MiniCard label="Active groups" value="3" accent="#35C974" />
          <MiniCard emoji="✈️" title="Lisbon weekend" sub="6 members · ₹1,842.30" />
          <MiniCard emoji="🏠" title="Apartment 4B" sub="3 members · ₹3,240.18" />
        </div>
      </div>
      <div style={{ position: 'relative', fontSize: 13, opacity: 0.6 }}>
        ★★★★★ "Used to dread the post-trip spreadsheet. Not anymore." — Priya S.
      </div>
    </div>
  )
}
