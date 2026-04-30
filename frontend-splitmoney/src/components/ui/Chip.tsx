export default function Chip({ children, variant = 'default', onClick, style = {} }) {
  const variants = {
    default: { background: 'white', color: 'var(--fg1)', border: '1px solid var(--border-default)' },
    owed:    { background: '#ECFBF2', color: '#0A9550' },
    owes:    { background: '#FFE5E5', color: '#C22222' },
    settled: { background: 'var(--sm-ink-100)', color: 'var(--sm-ink-600)' },
    dark:    { background: 'var(--sm-ink-950)', color: 'white' },
    brand:   { background: 'var(--sm-green-500)', color: 'white' },
  }

  return (
    <span
      onClick={onClick}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        padding: '6px 12px',
        borderRadius: 999,
        fontSize: 13,
        fontWeight: 600,
        cursor: onClick ? 'pointer' : 'default',
        ...variants[variant],
        ...style,
      }}
    >
      {children}
    </span>
  )
}
