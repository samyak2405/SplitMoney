import Icon from '../ui/Icon'
import NotificationBell from '../notifications/NotificationBell'

export default function TopBar({ title, subtitle, right, back }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '24px 40px',
        borderBottom: '1px solid var(--border-subtle)',
        background: 'rgba(255,255,255,0.9)',
        backdropFilter: 'blur(12px)',
        position: 'sticky',
        top: 0,
        zIndex: 10,
        gap: 24,
        flexWrap: 'wrap',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          minWidth: 0,
          flex: '1 1 auto',
        }}
      >
        {back}
        <div style={{ minWidth: 0 }}>
          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontWeight: 800,
              fontSize: 28,
              letterSpacing: '-0.025em',
              margin: 0,
              lineHeight: 1.1,
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              color: 'var(--fg1)',
            }}
          >
            {title}
          </h1>
          {subtitle && (
            <div
              style={{
                color: 'var(--fg3)',
                fontSize: 13,
                marginTop: 4,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {subtitle}
            </div>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0 }}>
        <div style={{ position: 'relative' }}>
          <Icon
            name="search"
            size={16}
            color="var(--fg3)"
            style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none' }}
          />
          <input
            placeholder="Search…"
            style={{
              padding: '10px 14px 10px 36px',
              borderRadius: 12,
              border: '1.5px solid var(--border-default)',
              background: 'var(--bg3)',
              fontSize: 13,
              width: 200,
              outline: 'none',
              fontFamily: 'var(--font-sans)',
              color: 'var(--fg1)',
            }}
          />
        </div>
        <NotificationBell />
        {right}
      </div>
    </div>
  )
}
