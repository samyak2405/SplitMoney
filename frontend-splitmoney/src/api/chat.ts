import { requestJson } from './http'
import { generateRequestId } from '../utils/requestMeta'
import type { DocumentItem } from './documents'

const CHAT_BASE_URL: string =
  (import.meta.env.VITE_CHAT_BASE_URL as string | undefined) ?? '/api/chat'

function chatRequest<T = unknown>(path: string, method: string, payload?: unknown) {
  return requestJson<T>(CHAT_BASE_URL, path, method, payload, {
    'X-Request-Id': generateRequestId(),
  })
}

export interface MessageItem {
  messageId: string
  groupId: string
  senderId: string
  senderEmail: string
  content: string
  msgType: string
  createdAtEpochMs: number
  documentMeta?: DocumentItem
}

export interface MessagesPage {
  messages: MessageItem[]
  nextCursor: string | null
}

export interface PresenceData {
  onlineUserIds: string[]
}

export interface UnreadCursorData {
  lastSeenEpochMs: number | null
}

export function fetchMessages(groupId: string, cursor?: string, limit = 50) {
  const q = new URLSearchParams({ limit: String(limit) })
  if (cursor) q.set('cursor', cursor)
  return chatRequest<MessagesPage>(`/v1/groups/${groupId}/messages?${q}`, 'GET')
}

export function fetchPresence(groupId: string) {
  return chatRequest<PresenceData>(`/v1/groups/${groupId}/presence`, 'GET')
}

export function fetchUnreadCursor(groupId: string) {
  return chatRequest<UnreadCursorData>(`/v1/groups/${groupId}/unread-cursor`, 'GET')
}

export function markGroupRead(groupId: string) {
  return chatRequest(`/v1/groups/${groupId}/mark-read`, 'POST')
}
