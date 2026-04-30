import { requestJson, requestJsonByUrl } from './http'
import { generateRequestId, getRequestMeta } from '../utils/requestMeta'
import type { ApiResult, Session } from '../types'

const DEVICE_FP_KEY = 'splitwise_device_fingerprint'

const USE_DOCKER_AUTH_UPSTREAM =
  String(import.meta.env.VITE_USE_DOCKER_AUTH_UPSTREAM ?? 'true').toLowerCase() === 'true'

const AUTH_BASE_URL: string =
  import.meta.env.VITE_AUTH_BASE_URL ??
  (USE_DOCKER_AUTH_UPSTREAM ? '/api/auth' : '/api-local/auth')

function authRequest<T = unknown>(path: string, method: string, payload?: unknown): Promise<ApiResult<T>> {
  if (AUTH_BASE_URL.startsWith('http')) {
    return requestJsonByUrl<T>(`${AUTH_BASE_URL}${path}`, method, payload)
  }
  return requestJson<T>(AUTH_BASE_URL, path, method, payload)
}

function getDeviceFingerprint(): string {
  try {
    const existing = localStorage.getItem(DEVICE_FP_KEY)
    if (existing) return existing
    const fp = `fp-${generateRequestId().replace('req-', '')}`
    localStorage.setItem(DEVICE_FP_KEY, fp)
    return fp
  } catch {
    return `fp-${Date.now()}`
  }
}

function getBrowserName(): string {
  const ua = navigator.userAgent
  if (ua.includes('Edg')) return 'Edge'
  if (ua.includes('Chrome')) return 'Chrome'
  if (ua.includes('Firefox')) return 'Firefox'
  if (ua.includes('Safari')) return 'Safari'
  return 'Browser'
}

export function registerUser({ email, mobile, password }: {
  email?: string; mobile?: string; password: string
}) {
  return authRequest('/v1/register', 'POST', {
    ...getRequestMeta(), email, mobile: mobile ?? null, password, mfaMethod: null,
  })
}

export function loginUser({ email, password }: { email: string; password: string }) {
  return authRequest('/v1/login', 'POST', {
    ...getRequestMeta(), email, password,
    deviceName: `${getBrowserName()}-${navigator.platform || 'UnknownOS'}`,
    deviceFingerprint: getDeviceFingerprint(),
  })
}

export function logoutUser() {
  return authRequest('/v1/logout', 'POST')
}

export function getCurrentSession(): Promise<ApiResult<Session>> {
  return authRequest<Session>('/v1/session', 'GET')
}

export function loginWithGoogleCode({ authorizationCode, redirectUri, codeVerifier, nonce }: {
  authorizationCode: string; redirectUri: string; codeVerifier: string; nonce: string
}) {
  return authRequest<Session>('/v1/login/google', 'POST', {
    ...getRequestMeta(), provider: 'GOOGLE', authorizationCode, redirectUri, codeVerifier, nonce,
  })
}

export function verifyRegistrationOtp({ email, otp }: { email: string; otp: string }) {
  return authRequest('/v1/verify-registration-otp', 'POST', { ...getRequestMeta(), email, otp })
}

export function resendRegistrationOtp({ email, mobile, otpChannel }: {
  email?: string; mobile?: string; otpChannel?: string
}) {
  return authRequest('/v1/resend-registration-otp', 'POST', {
    ...getRequestMeta(), email, mobile: mobile ?? null, otpChannel: otpChannel ?? null,
  })
}

export function forgotPassword(payload: Record<string, unknown>) {
  return authRequest('/v1/forgot-password', 'POST', payload)
}

export function resetPassword(payload: Record<string, unknown>) {
  return authRequest('/v1/reset-password', 'POST', payload)
}
