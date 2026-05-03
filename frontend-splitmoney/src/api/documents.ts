import { generateRequestId } from '../utils/requestMeta'

const DOCS_BASE_URL: string =
  (import.meta.env.VITE_DOCS_BASE_URL as string | undefined) ?? '/api/docs'

export interface DocumentItem {
  documentId: string
  fileName: string
  fileSize: number
  mimeType: string
  url: string
  uploaderEmail: string
  createdAt: number
}

interface ApiBody<T> {
  success: boolean
  responseMessage?: string
  data?: T
}

async function docsRequest<T>(path: string, method: string, body?: BodyInit, contentType?: string): Promise<{ ok: boolean; body?: ApiBody<T> }> {
  const headers: Record<string, string> = { 'X-Request-Id': generateRequestId() }
  if (contentType) headers['Content-Type'] = contentType

  const res = await fetch(`${DOCS_BASE_URL}${path}`, { method, headers, credentials: 'include', body })
  const json: ApiBody<T> = await res.json()
  return { ok: res.ok, body: json }
}

export async function uploadDocument(groupId: string, file: File): Promise<{ ok: boolean; body?: ApiBody<DocumentItem> }> {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`${DOCS_BASE_URL}/v1/groups/${groupId}/upload`, {
    method: 'POST',
    headers: { 'X-Request-Id': generateRequestId() },
    credentials: 'include',
    body: form,
  })
  const json: ApiBody<DocumentItem> = await res.json()
  return { ok: res.ok, body: json }
}

export function listDocuments(groupId: string, page = 0) {
  return docsRequest<DocumentItem[]>(`/v1/groups/${groupId}/documents?page=${page}&size=50`, 'GET')
}

export function deleteDocument(documentId: string) {
  return docsRequest<void>(`/v1/documents/${documentId}`, 'DELETE')
}
