interface PresenceDotProps {
  isOnline: boolean
  size?: number
}

export default function PresenceDot({ isOnline, size = 10 }: PresenceDotProps) {
  return (
    <span
      style={{
        display: 'inline-block',
        width: size,
        height: size,
        borderRadius: '50%',
        background: isOnline ? 'var(--sm-green-600, #12B35E)' : 'var(--border-default, #D1D5DB)',
        border: '2px solid white',
        flexShrink: 0,
      }}
      title={isOnline ? 'Online' : 'Offline'}
    />
  )
}
