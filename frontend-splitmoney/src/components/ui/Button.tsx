export default function Button({
  variant = 'primary',
  size = 'md',
  children,
  onClick,
  style = {},
  disabled,
  type = 'button',
  className = '',
}) {
  const base = {
    border: 'none',
    cursor: disabled ? 'not-allowed' : 'pointer',
    fontFamily: 'var(--font-sans)',
    fontWeight: 600,
    borderRadius: 999,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    transition: 'all 180ms var(--ease-out)',
    opacity: disabled ? 0.4 : 1,
    fontSize: size === 'lg' ? 16 : size === 'sm' ? 13 : 14,
    padding: size === 'lg' ? '14px 26px' : size === 'sm' ? '8px 14px' : '10px 18px',
    whiteSpace: 'nowrap',
  }

  const variants = {
    primary: {
      background: 'var(--grad-brand)',
      color: '#03241A',
      boxShadow: '0 4px 12px rgba(18,179,94,.32), inset 0 1px 0 rgba(255,255,255,.4)',
    },
    dark: { background: 'var(--sm-ink-950)', color: 'white' },
    secondary: { background: 'var(--sm-green-50)', color: 'var(--sm-green-700)' },
    ghost: {
      background: 'transparent',
      color: 'var(--fg1)',
      border: '1.5px solid var(--border-strong)',
    },
    lime: {
      background: 'var(--sm-lime-400)',
      color: '#03241A',
      boxShadow: '0 4px 16px rgba(168,232,20,.4)',
    },
    danger: { background: 'var(--sm-danger)', color: 'white' },
    white: {
      background: 'white',
      color: 'var(--fg1)',
      border: '1.5px solid var(--border-default)',
    },
  }

  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={className}
      style={{ ...base, ...variants[variant], ...style }}
    >
      {children}
    </button>
  )
}
