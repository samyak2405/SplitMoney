import { Fragment, useEffect, useRef } from 'react'
import { useNotifications } from '../../context/NotificationContext'
import Icon from '../ui/Icon'

const EVENT_ICON = {
  EXPENSE_CREATED: 'receipt',
  EXPENSE_ADDED_AGAINST_USER: 'receipt',
  SETTLEMENT_DUE: 'card',
  PAYMENT_RECEIVED: 'zap',
  GROUP_MEMBER_ADDED: 'users',
  GROUP_MEMBER_REMOVED: 'users',
  REGISTRATION_OTP: 'mail',
  EMAIL_VERIFICATION: 'mail',
  PASSWORD_RESET: 'shield',
}

const EVENT_COLOR = {
  EXPENSE_CREATED: 'var(--sm-green-500)',
  EXPENSE_ADDED_AGAINST_USER: 'var(--sm-green-500)',
  SETTLEMENT_DUE: 'var(--sm-warning)',
  PAYMENT_RECEIVED: 'var(--sm-green-600)',
  GROUP_MEMBER_ADDED: 'var(--sm-info)',
  GROUP_MEMBER_REMOVED: 'var(--sm-ink-400)',
  REGISTRATION_OTP: '#8B5CF6',
  EMAIL_VERIFICATION: '#8B5CF6',
  PASSWORD_RESET: '#8B5CF6',
}

function EventIcon({ eventType }) {
  const icon = EVENT_ICON[eventType] || 'bell'
  const color = EVENT_COLOR[eventType] || 'var(--fg3)'
  const bg = color + '18'
  return (
    <div style={{
      width: 36,
      height: 36,
      borderRadius: 10,
      background: bg,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
    }}>
      <Icon name={icon} size={16} color={color} />
    </div>
  )
}

export default function NotificationPanel({ style }) {
  const {
    notifications,
    isLoading,
    isFetchingMore,
    hasMore,
    markRead,
    markAllRead,
    loadMore,
  } = useNotifications()

  const listRef = useRef(null)

  // Infinite scroll
  useEffect(() => {
    const node = listRef.current
    if (!node) return
    const onScroll = () => {
      if (isFetchingMore || !hasMore) return
      if (node.scrollHeight - node.scrollTop - node.clientHeight < 64) loadMore()
    }
    node.addEventListener('scroll', onScroll)
    return () => node.removeEventListener('scroll', onScroll)
  }, [isFetchingMore, hasMore, loadMore])

  const unreadCount = notifications.filter(n => !n.isRead).length

  return (
    <div style={{
      width: 380,
      maxHeight: 520,
      background: 'var(--sm-white)',
      borderRadius: 16,
      border: '1px solid var(--border-default)',
      boxShadow: 'var(--shadow-xl)',
      display: 'flex',
      flexDirection: 'column',
      overflow: 'hidden',
      ...style,
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '16px 20px 14px',
        borderBottom: '1px solid var(--border-subtle)',
        flexShrink: 0,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontWeight: 700, fontSize: 15, color: 'var(--fg1)' }}>Notifications</span>
          {unreadCount > 0 && (
            <span style={{
              background: 'var(--sm-green-500)',
              color: 'white',
              fontSize: 11,
              fontWeight: 700,
              borderRadius: 999,
              padding: '1px 7px',
              lineHeight: '18px',
            }}>
              {unreadCount}
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllRead}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: 12,
              color: 'var(--fg-brand)',
              fontWeight: 600,
              padding: '4px 8px',
              borderRadius: 8,
              fontFamily: 'inherit',
            }}
          >
            Mark all read
          </button>
        )}
      </div>

      {/* Body */}
      {isLoading ? (
        <div style={{ padding: '40px 20px', textAlign: 'center', color: 'var(--fg3)', fontSize: 13 }}>
          Loading notifications…
        </div>
      ) : notifications.length === 0 ? (
        <div style={{ padding: '48px 20px', textAlign: 'center' }}>
          <div style={{
            width: 48,
            height: 48,
            borderRadius: 14,
            background: 'var(--bg3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 12px',
          }}>
            <Icon name="bell" size={22} color="var(--fg3)" />
          </div>
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--fg2)', marginBottom: 4 }}>All caught up</div>
          <div style={{ fontSize: 13, color: 'var(--fg3)' }}>No notifications yet.</div>
        </div>
      ) : (
        <ul ref={listRef} style={{ margin: 0, padding: '8px 0', listStyle: 'none', overflowY: 'auto', flex: 1 }}>
          {notifications.map((item, i) => {
            const showDate = i === 0 || item.dateKey !== notifications[i - 1].dateKey
            return (
              <Fragment key={item.id}>
                {showDate && (
                  <li style={{
                    padding: '8px 20px 4px',
                    fontSize: 11,
                    fontWeight: 700,
                    color: 'var(--fg3)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.06em',
                  }}>
                    {item.date}
                  </li>
                )}
                <li
                  style={{
                    display: 'flex',
                    gap: 12,
                    padding: '10px 20px',
                    background: item.isRead ? 'transparent' : 'rgba(18,179,94,0.04)',
                    cursor: item.isRead ? 'default' : 'pointer',
                    transition: 'background 120ms',
                    position: 'relative',
                  }}
                  onClick={() => { if (!item.isRead) markRead(item.id) }}
                >
                  <EventIcon eventType={item.eventType} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      justifyContent: 'space-between',
                      gap: 8,
                    }}>
                      <span style={{
                        fontSize: 13,
                        fontWeight: item.isRead ? 500 : 700,
                        color: 'var(--fg1)',
                        lineHeight: 1.4,
                      }}>
                        {item.title}
                      </span>
                      {!item.isRead && (
                        <span style={{
                          width: 7,
                          height: 7,
                          borderRadius: 999,
                          background: 'var(--sm-green-500)',
                          flexShrink: 0,
                          marginTop: 4,
                        }} />
                      )}
                    </div>
                    <div style={{
                      fontSize: 12,
                      color: 'var(--fg2)',
                      marginTop: 2,
                      lineHeight: 1.5,
                      overflow: 'hidden',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                    }}>
                      {item.message}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--fg3)', marginTop: 4 }}>
                      {item.time}
                    </div>
                  </div>
                </li>
              </Fragment>
            )
          })}

          {isFetchingMore && (
            <li style={{ padding: '12px 20px', textAlign: 'center', fontSize: 12, color: 'var(--fg3)' }}>
              Loading more…
            </li>
          )}
          {!hasMore && notifications.length > 0 && (
            <li style={{ padding: '12px 20px', textAlign: 'center', fontSize: 12, color: 'var(--fg3)' }}>
              You're all caught up
            </li>
          )}
        </ul>
      )}
    </div>
  )
}
