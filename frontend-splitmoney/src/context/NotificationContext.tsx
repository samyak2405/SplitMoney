import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { Client } from '@stomp/stompjs'
import { useAuth } from './AuthContext'
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationsRead,
} from '../api/notifications'
import type { NotificationContextValue, NotificationItem } from '../types'

const NotificationContext = createContext<NotificationContextValue | null>(null)

export function useNotifications(): NotificationContextValue {
  const ctx = useContext(NotificationContext)
  if (!ctx) throw new Error('useNotifications must be used inside NotificationProvider')
  return ctx
}

const POLL_MS = 30_000

function formatDate(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const todayMs = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const dayMs = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  if (dayMs === todayMs) return 'Today'
  if (dayMs === todayMs - 86_400_000) return 'Yesterday'
  return d.toLocaleDateString(undefined, {
    month: 'short', day: 'numeric',
    ...(d.getFullYear() !== now.getFullYear() && { year: 'numeric' }),
  })
}

function formatDateKey(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

function mapNotification(item: Record<string, unknown>): NotificationItem {
  return {
    id: item.id as string,
    eventType: item.eventType as string,
    title: item.title as string,
    message: item.body as string,
    isRead: Boolean(item.isRead),
    createdAt: item.createdAt as string,
    date: formatDate(item.createdAt as string),
    dateKey: formatDateKey(item.createdAt as string),
    time: formatTime(item.createdAt as string),
    payload: (item.payload as Record<string, unknown>) || {},
  }
}

const WS_URL = (() => {
  const explicit = import.meta.env.VITE_NOTIFICATION_WS_URL as string | undefined
  if (explicit) return explicit
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${window.location.host}/ws-notifications`
})()

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth()
  const userId = session?.userId

  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [isFetchingMore, setIsFetchingMore] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [cursor, setCursor] = useState<string | null>(null)
  const [isPanelOpen, setIsPanelOpen] = useState(false)

  const isPanelOpenRef = useRef(false)
  isPanelOpenRef.current = isPanelOpen
  const loadNotificationsRef = useRef<(() => Promise<void>) | null>(null)

  const refreshUnreadCount = useCallback(async () => {
    if (!userId) return
    const result = await getUnreadNotificationCount()
    if (result.ok && result.body?.success) {
      setUnreadCount((result.body.data as { unreadCount?: number })?.unreadCount ?? 0)
    }
  }, [userId])

  const loadNotifications = useCallback(async () => {
    if (!userId) return
    setIsLoading(true)
    try {
      const result = await listNotifications({ cursor: null, limit: 20 })
      if (result.ok && result.body?.success) {
        const items = (result.body.data as { items?: unknown[]; nextCursor?: string } | undefined)
        setNotifications((items?.items ?? []).map(i => mapNotification(i as Record<string, unknown>)))
        const next = items?.nextCursor ?? null
        setCursor(next)
        setHasMore(Boolean(next))
      }
    } finally {
      setIsLoading(false)
    }
  }, [userId])

  loadNotificationsRef.current = loadNotifications

  const loadMore = useCallback(async () => {
    if (!userId || !cursor || isFetchingMore) return
    setIsFetchingMore(true)
    try {
      const result = await listNotifications({ cursor, limit: 20 })
      if (result.ok && result.body?.success) {
        const items = (result.body.data as { items?: unknown[]; nextCursor?: string } | undefined)
        setNotifications(prev => [
          ...prev,
          ...(items?.items ?? []).map(i => mapNotification(i as Record<string, unknown>)),
        ])
        const next = items?.nextCursor ?? null
        setCursor(next)
        setHasMore(Boolean(next))
      }
    } finally {
      setIsFetchingMore(false)
    }
  }, [userId, cursor, isFetchingMore])

  const markRead = useCallback(async (id: string) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n))
    setUnreadCount(c => Math.max(0, c - 1))
    const result = await markNotificationsRead({ ids: [id] })
    if (!result.ok) {
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: false } : n))
      setUnreadCount(c => c + 1)
    }
  }, [])

  const markAllRead = useCallback(async () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
    setUnreadCount(0)
    const result = await markAllNotificationsRead()
    if (!result.ok) {
      await refreshUnreadCount()
      await loadNotifications()
    }
  }, [refreshUnreadCount, loadNotifications])

  const openPanel = useCallback(() => { setIsPanelOpen(true); loadNotifications() }, [loadNotifications])
  const closePanel = useCallback(() => setIsPanelOpen(false), [])
  const togglePanel = useCallback(() => {
    if (isPanelOpenRef.current) closePanel(); else openPanel()
  }, [openPanel, closePanel])

  useEffect(() => {
    if (!userId) return
    refreshUnreadCount()
    const id = window.setInterval(refreshUnreadCount, POLL_MS)
    return () => window.clearInterval(id)
  }, [userId, refreshUnreadCount])

  useEffect(() => {
    if (!userId) return
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/users/${userId}/notifications`, (msg) => {
          try {
            const push = JSON.parse(msg.body) as { unreadCount?: number }
            if (push.unreadCount != null) setUnreadCount(push.unreadCount)
            if (isPanelOpenRef.current) loadNotificationsRef.current?.()
          } catch { /* ignore malformed push */ }
        })
      },
    })
    client.activate()
    return () => { client.deactivate() }
  }, [userId]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!userId) {
      setNotifications([]); setUnreadCount(0); setIsPanelOpen(false)
      setCursor(null); setHasMore(false)
    }
  }, [userId])

  return (
    <NotificationContext.Provider value={{
      notifications, unreadCount, isLoading, isFetchingMore, hasMore,
      isPanelOpen, openPanel, closePanel, togglePanel, loadMore, markRead, markAllRead,
    }}>
      {children}
    </NotificationContext.Provider>
  )
}
