import { useEffect, useRef } from 'react'
import { useChatContext } from '../../context/ChatContext'
import MessageBubble from './MessageBubble'
import { useAuth } from '../../context/AuthContext'

interface MessageListProps {
  groupId: string
}

function UnreadDivider({ count }: { count: number }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '10px 0', margin: '6px 0',
    }}>
      <div style={{ flex: 1, height: 1, background: 'linear-gradient(to right, transparent, #818cf8)' }} />
      <span style={{
        fontSize: 11, fontWeight: 700,
        color: '#4f46e5',
        whiteSpace: 'nowrap', letterSpacing: '0.05em', textTransform: 'uppercase',
        background: '#eef2ff',
        padding: '3px 12px',
        borderRadius: 100,
        border: '1px solid #c7d2fe',
      }}>
        {count} new message{count !== 1 ? 's' : ''}
      </span>
      <div style={{ flex: 1, height: 1, background: 'linear-gradient(to left, transparent, #818cf8)' }} />
    </div>
  )
}

function DateSeparator({ label }: { label: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0' }}>
      <div style={{ flex: 1, height: 1, background: 'var(--border-subtle, #F3F4F6)' }} />
      <span style={{
        fontSize: 11, color: 'var(--fg3)', whiteSpace: 'nowrap',
        padding: '2px 10px', background: 'var(--bg1, #fff)',
        border: '1px solid var(--border-subtle, #F3F4F6)', borderRadius: 100,
      }}>
        {label}
      </span>
      <div style={{ flex: 1, height: 1, background: 'var(--border-subtle, #F3F4F6)' }} />
    </div>
  )
}

function formatDateLabel(epochMs: number): string {
  const d = new Date(epochMs)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)
  if (d.toDateString() === today.toDateString()) return 'Today'
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday'
  return d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })
}

export default function MessageList({ groupId: _ }: MessageListProps) {
  const { messages, nextCursor, loadingHistory, loadMore, lastSeenEpochMs } = useChatContext()
  const { session } = useAuth()
  const bottomRef = useRef<HTMLDivElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const prevScrollHeightRef = useRef<number>(0)
  const isNearBottomRef = useRef(true)
  const didScrollToUnreadRef = useRef(false)
  const unreadDividerRef = useRef<HTMLDivElement>(null)

  // Only messages from OTHER users count as unread
  const firstUnreadIndex = lastSeenEpochMs != null
    ? messages.findIndex(m => m.createdAtEpochMs > lastSeenEpochMs && m.senderId !== session?.userId)
    : -1

  const unreadCount = lastSeenEpochMs != null
    ? messages.filter(m => m.createdAtEpochMs > lastSeenEpochMs && m.senderId !== session?.userId).length
    : 0

  useEffect(() => {
    if (loadingHistory) return
    if (firstUnreadIndex > 0 && !didScrollToUnreadRef.current && unreadDividerRef.current) {
      unreadDividerRef.current.scrollIntoView({ block: 'center' })
      didScrollToUnreadRef.current = true
    } else if (isNearBottomRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages, loadingHistory])

  useEffect(() => {
    const container = containerRef.current
    if (!container) return
    const newScrollHeight = container.scrollHeight
    const diff = newScrollHeight - prevScrollHeightRef.current
    if (diff > 0 && prevScrollHeightRef.current > 0) {
      container.scrollTop += diff
    }
    prevScrollHeightRef.current = newScrollHeight
  }, [messages.length])

  const handleScroll = () => {
    const container = containerRef.current
    if (!container) return
    const distFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight
    isNearBottomRef.current = distFromBottom < 60

    if (container.scrollTop < 60 && nextCursor && !loadingHistory) {
      prevScrollHeightRef.current = container.scrollHeight
      loadMore()
    }
  }

  if (loadingHistory && messages.length === 0) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: 'var(--fg3)', fontSize: 14 }}>
        Loading messages…
      </div>
    )
  }

  return (
    <div
      ref={containerRef}
      onScroll={handleScroll}
      style={{
        flex: 1,
        overflowY: 'auto',
        padding: '8px 16px 12px',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {loadingHistory && (
        <div style={{ textAlign: 'center', color: 'var(--fg3)', fontSize: 12, padding: '8px 0' }}>
          Loading older messages…
        </div>
      )}
      {!nextCursor && messages.length > 0 && (
        <div style={{ textAlign: 'center', color: 'var(--fg3)', fontSize: 11, padding: '8px 0 4px' }}>
          — beginning of conversation —
        </div>
      )}
      {messages.length === 0 && !loadingHistory && (
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', color: 'var(--fg3)', gap: 8 }}>
          <span style={{ fontSize: 40 }}>💬</span>
          <span style={{ fontSize: 14, fontWeight: 500 }}>No messages yet. Say hello!</span>
        </div>
      )}

      {messages.map((msg, i) => {
        const prev = messages[i - 1]
        const isMine = msg.senderId === session?.userId
        const showSender = !prev || prev.senderId !== msg.senderId
        const isFirstUnread = i === firstUnreadIndex

        const prevDateStr = prev ? new Date(prev.createdAtEpochMs).toDateString() : null
        const thisDateStr = new Date(msg.createdAtEpochMs).toDateString()
        const showDate = !prevDateStr || prevDateStr !== thisDateStr

        const topMargin = showDate ? 0 : (showSender && i > 0) ? 10 : 2

        return (
          <div key={msg.messageId} style={{ marginTop: topMargin }}>
            {showDate && <DateSeparator label={formatDateLabel(msg.createdAtEpochMs)} />}
            {isFirstUnread && (
              <div ref={unreadDividerRef}>
                <UnreadDivider count={unreadCount} />
              </div>
            )}
            <MessageBubble message={msg} isMine={isMine} showSender={showSender} />
          </div>
        )
      })}
      <div ref={bottomRef} />
    </div>
  )
}
