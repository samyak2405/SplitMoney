export default function Card({ children, style = {}, onClick, className = '' }) {
  return (
    <div
      onClick={onClick}
      className={className}
      style={{
        background: 'white',
        borderRadius: 20,
        border: '1px solid var(--border-subtle)',
        boxShadow: 'var(--shadow-sm)',
        cursor: onClick ? 'pointer' : 'default',
        transition: onClick ? 'all 180ms var(--ease-out)' : undefined,
        ...style,
      }}
    >
      {children}
    </div>
  )
}
