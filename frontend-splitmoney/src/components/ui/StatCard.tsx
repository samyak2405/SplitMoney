export default function StatCard({ label, value, sub, accent = 'var(--fg1)', dark, children }) {
  return (
    <div
      style={{
        background: dark ? 'var(--sm-ink-950)' : 'white',
        color: dark ? 'white' : 'inherit',
        borderRadius: 24,
        padding: 28,
        border: dark ? 'none' : '1px solid var(--border-subtle)',
        position: 'relative',
        overflow: 'hidden',
        boxShadow: dark ? 'none' : 'var(--shadow-sm)',
      }}
    >
      {dark && (
        <div
          style={{
            position: 'absolute',
            inset: '-20%',
            background: 'var(--grad-glow)',
          }}
        />
      )}
      <div style={{ position: 'relative' }}>
        <div
          style={{
            fontSize: 12,
            textTransform: 'uppercase',
            letterSpacing: '0.1em',
            color: dark ? 'rgba(255,255,255,0.6)' : 'var(--fg3)',
            fontWeight: 600,
          }}
        >
          {label}
        </div>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontWeight: 800,
            fontSize: 'clamp(28px, 3.2vw, 44px)',
            letterSpacing: '-0.03em',
            color: accent,
            fontVariantNumeric: 'tabular-nums',
            marginTop: 8,
            lineHeight: 1,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {value}
        </div>
        {sub && (
          <div
            style={{
              fontSize: 13,
              color: dark ? 'rgba(255,255,255,0.6)' : 'var(--fg3)',
              marginTop: 8,
            }}
          >
            {sub}
          </div>
        )}
        {children}
      </div>
    </div>
  )
}
