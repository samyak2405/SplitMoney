import { requestJson } from './http'
import { generateRequestId } from '../utils/requestMeta'

const NOTIFICATION_BASE_URL: string =
  import.meta.env.VITE_NOTIFICATION_BASE_URL ?? '/api/notifications'

function notifRequest<T = unknown>(path: string, method: string, payload?: unknown) {
  return requestJson<T>(NOTIFICATION_BASE_URL, path, method, payload, {
    'X-Request-Id': generateRequestId(),
  })
}

export function listNotifications({ cursor = null, limit = 20, status = 'all' }: {
  cursor?: string | null; limit?: number; status?: string
} = {}) {
  const q = new URLSearchParams({ limit: String(limit), status })
  if (cursor) q.set('cursor', cursor)
  return notifRequest(`/v1/list?${q}`, 'GET')
}

export function getUnreadNotificationCount() {
  return notifRequest<{ unreadCount: number }>('/v1/unread-count', 'GET')
}

export function markNotificationsRead({ ids }: { ids: string[] }) {
  return notifRequest('/v1/mark-read', 'POST', { ids })
}

export function markAllNotificationsRead() {
  return notifRequest('/v1/mark-all-read', 'POST')
}
