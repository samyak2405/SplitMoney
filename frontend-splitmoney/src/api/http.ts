import type { ApiResult } from '../types'

// ── Global 401 handler ────────────────────────────────────────────────────────
// Registered once by AuthProvider. Every request that gets a 401 back will
// call this so components never need to check status === 401 individually.
type Handler401 = (body: unknown) => void
let _handler401: Handler401 | null = null

export function registerUnauthorizedHandler(fn: Handler401): void {
  _handler401 = fn
}

export function clearUnauthorizedHandler(): void {
  _handler401 = null
}

// ── Helpers ───────────────────────────────────────────────────────────────────
async function parseJson(response: Response): Promise<unknown> {
  try { return await response.json() } catch { return null }
}

function buildResult<T>(response: Response, body: unknown): ApiResult<T> {
  return { ok: response.ok, status: response.status, body: body as ApiResult<T>['body'] }
}

async function handleResponse<T>(response: Response): Promise<ApiResult<T>> {
  const body = await parseJson(response)
  if (response.status === 401 && _handler401) {
    _handler401(body)
  }
  return buildResult<T>(response, body)
}

// ── Public fetch wrappers ─────────────────────────────────────────────────────
export async function requestJson<T = unknown>(
  baseUrl: string,
  path: string,
  method: string,
  payload?: unknown,
  extraHeaders: Record<string, string> = {},
): Promise<ApiResult<T>> {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...extraHeaders },
    body: payload == null ? undefined : JSON.stringify(payload),
  })
  return handleResponse<T>(response)
}

export async function requestJsonByUrl<T = unknown>(
  url: string,
  method: string,
  payload?: unknown,
  extraHeaders: Record<string, string> = {},
): Promise<ApiResult<T>> {
  const response = await fetch(url, {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...extraHeaders },
    body: payload == null ? undefined : JSON.stringify(payload),
  })
  return handleResponse<T>(response)
}
