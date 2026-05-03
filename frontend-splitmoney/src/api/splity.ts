import { requestJson } from './http'

const AI_BASE_URL = (import.meta.env.VITE_AI_BASE_URL as string | undefined) ?? '/api/ai'

function aiRequest<T = unknown>(path: string, method: string, payload?: unknown) {
  return requestJson<T>(AI_BASE_URL, path, method, payload)
}

export interface SplityMessage {
  messageId: string
  role: 'user' | 'assistant'
  content: string
  createdAtEpochMs: number
  expenseCreated?: boolean
}

export interface AttachmentInfo {
  documentId: string
  documentMimeType: string
  documentName: string
  documentBase64?: string // base64-encoded file bytes sent directly to avoid service-to-service fetch
}

export function fetchSplityHistory(groupId: string) {
  return aiRequest<SplityMessage[]>(`/v1/groups/${groupId}/splity/messages`, 'GET')
}

export function sendSplityMessage(groupId: string, content: string, attachment?: AttachmentInfo) {
  return aiRequest<SplityMessage>(`/v1/groups/${groupId}/splity/chat`, 'POST', {
    content,
    ...(attachment ?? {}),
  })
}
