export default function Avatar({ name = '', size = 36, bg, emoji, children, style = {} }) {
  const initial = (name || '').split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase() || '?'
  const palette = ['#FF7A66', '#9C7BFF', '#5EC8F2', '#34D6B8', '#FFD85C', '#FF8AC7', '#FFB26B']
  const auto = bg || palette[(name.charCodeAt(0) || 0) % palette.length]
  const isDark = ['#FFD85C', '#FF8AC7', '#FFB26B'].includes(auto)

  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: 999,
        background: auto,
        color: isDark ? '#03241A' : 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontWeight: 700,
        fontSize: size * 0.38,
        flexShrink: 0,
        fontFamily: 'var(--font-sans)',
        ...style,
      }}
    >
      {emoji || children || initial}
    </div>
  )
}

export function AvatarStack({ people = [], size = 28, max = 4 }) {
  const shown = people.slice(0, max)
  const extra = people.length - shown.length

  return (
    <div className="flex">
      {shown.map((p, i) => (
        <div
          key={i}
          style={{ marginLeft: i === 0 ? 0 : -size * 0.32, border: '2px solid white', borderRadius: 999 }}
        >
          <Avatar {...p} size={size} />
        </div>
      ))}
      {extra > 0 && (
        <div style={{ marginLeft: -size * 0.32, border: '2px solid white', borderRadius: 999 }}>
          <Avatar bg="#EAEEEC" size={size} style={{ color: '#435049' }}>
            +{extra}
          </Avatar>
        </div>
      )}
    </div>
  )
}
