import { useChatContext } from '../../context/ChatContext'
import PresenceDot from './PresenceDot'

interface PresenceBarProps {
  members: Array<{ email: string }>
}

export default function PresenceBar({ members }: PresenceBarProps) {
  const { onlineUserIds, isConnected } = useChatContext()

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8,
      padding: '8px 16px',
      borderBottom: '1px solid var(--border-subtle)',
      background: 'white',
      fontSize: 12,
      color: 'var(--fg3)',
      flexWrap: 'wrap',
    }}>
      <PresenceDot isOnline={isConnected} size={8} />
      <span style={{ color: isConnected ? 'var(--sm-green-600)' : 'var(--fg3)' }}>
        {isConnected ? 'Connected' : 'Connecting…'}
      </span>
      {onlineUserIds.size > 0 && (
        <>
          <span style={{ color: 'var(--border-default)' }}>·</span>
          <span>{onlineUserIds.size} online</span>
        </>
      )}
    </div>
  )
}
