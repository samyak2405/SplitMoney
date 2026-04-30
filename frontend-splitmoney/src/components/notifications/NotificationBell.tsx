import { createPortal } from 'react-dom'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useNotifications } from '../../context/NotificationContext'
import NotificationPanel from './NotificationPanel'
import Icon from '../ui/Icon'

export default function NotificationBell() {
  const { unreadCount, isPanelOpen, togglePanel, closePanel } = useNotifications()
  const bellRef = useRef(null)
  const panelRef = useRef(null)
  const [panelPos, setPanelPos] = useState({ top: 0, right: 0 })

  const recalcPos = useCallback(() => {
    if (bellRef.current) {
      const rect = bellRef.current.getBoundingClientRect()
      setPanelPos({
        top: rect.bottom + 8,
        right: window.innerWidth - rect.right,
      })
    }
  }, [])

  // Recalculate position when panel opens
  useEffect(() => {
    if (isPanelOpen) recalcPos()
  }, [isPanelOpen, recalcPos])

  // Close on click outside
  useEffect(() => {
    if (!isPanelOpen) return
    const onMouseDown = (e) => {
      const inBell = bellRef.current?.contains(e.target)
      const inPanel = panelRef.current?.contains(e.target)
      if (!inBell && !inPanel) closePanel()
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [isPanelOpen, closePanel])

  const badgeCount = Math.min(unreadCount, 99)

  return (
    <>
      <button
        ref={bellRef}
        onClick={togglePanel}
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
        style={{
          width: 40,
          height: 40,
          borderRadius: 12,
          border: isPanelOpen
            ? '1.5px solid var(--sm-green-400)'
            : '1.5px solid var(--border-default)',
          background: isPanelOpen ? 'var(--sm-green-50)' : 'white',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          flexShrink: 0,
          transition: 'border-color 120ms, background 120ms',
        }}
      >
        <Icon
          name="bell"
          size={18}
          color={isPanelOpen ? 'var(--sm-green-600)' : 'var(--fg2)'}
        />
        {unreadCount > 0 && (
          <span style={{
            position: 'absolute',
            top: -4,
            right: -4,
            minWidth: 18,
            height: 18,
            padding: '0 4px',
            borderRadius: 999,
            background: 'var(--sm-danger)',
            border: '2px solid white',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 10,
            fontWeight: 700,
            color: 'white',
            lineHeight: 1,
          }}>
            {badgeCount}{unreadCount > 99 ? '+' : ''}
          </span>
        )}
      </button>

      {isPanelOpen && createPortal(
        <div ref={panelRef} style={{ position: 'fixed', top: panelPos.top, right: panelPos.right, zIndex: 1000 }}>
          <NotificationPanel />
        </div>,
        document.body
      )}
    </>
  )
}
