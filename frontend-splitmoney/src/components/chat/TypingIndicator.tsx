interface TypingIndicatorProps {
  typingUserEmails: Set<string>
  currentUserEmail: string
}

export default function TypingIndicator({ typingUserEmails, currentUserEmail }: TypingIndicatorProps) {
  const others = [...typingUserEmails].filter(e => e !== currentUserEmail)
  if (others.length === 0) return null

  const names = others.map(e => e.split('@')[0])
  const label = names.length === 1
    ? `${names[0]} is typing`
    : names.length === 2
      ? `${names[0]} and ${names[1]} are typing`
      : `${names[0]} and ${names.length - 1} others are typing`

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '4px 16px', minHeight: 24 }}>
      <div style={{ display: 'flex', gap: 3, alignItems: 'center' }}>
        {[0, 1, 2].map(i => (
          <span
            key={i}
            style={{
              width: 5, height: 5, borderRadius: '50%',
              background: 'var(--fg3)',
              display: 'inline-block',
              animation: `typingBounce 1.2s ease-in-out ${i * 0.2}s infinite`,
            }}
          />
        ))}
      </div>
      <span style={{ fontSize: 12, color: 'var(--fg3)', fontStyle: 'italic' }}>
        {label}…
      </span>
      <style>{`
        @keyframes typingBounce {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-4px); }
        }
      `}</style>
    </div>
  )
}
