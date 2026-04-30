export function generateRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `req-${crypto.randomUUID()}`
  }
  return `req-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function generateId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

export function getRequestMeta() {
  return {
    requestId: generateRequestId(),
    ipAddress: '127.0.0.1',
    userAgent: navigator.userAgent,
  }
}
