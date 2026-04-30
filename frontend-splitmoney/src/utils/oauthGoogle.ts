const OAUTH_CONTEXT_KEY = 'splitwise_google_oauth_context'

interface OAuthContext {
  state: string
  nonce: string
  codeVerifier: string
  redirectUri: string
  createdAt: number
}

function toBase64Url(bytes: Uint8Array): string {
  const binary = String.fromCharCode(...bytes)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

function randomString(byteLength = 32): string {
  const bytes = new Uint8Array(byteLength)
  crypto.getRandomValues(bytes)
  return toBase64Url(bytes)
}

async function sha256(input: string): Promise<ArrayBuffer> {
  const data = new TextEncoder().encode(input)
  return crypto.subtle.digest('SHA-256', data)
}

async function createCodeChallenge(codeVerifier: string): Promise<string> {
  const digest = await sha256(codeVerifier)
  return toBase64Url(new Uint8Array(digest))
}

export async function createGoogleAuthorizationUrl({ clientId, redirectUri }: {
  clientId: string; redirectUri: string
}): Promise<string> {
  if (!clientId) throw new Error('Google OAuth client ID is missing. Set VITE_GOOGLE_CLIENT_ID.')

  const codeVerifier = randomString(64)
  const codeChallenge = await createCodeChallenge(codeVerifier)
  const state = randomString(24)
  const nonce = randomString(24)

  const context: OAuthContext = { state, nonce, codeVerifier, redirectUri, createdAt: Date.now() }
  sessionStorage.setItem(OAUTH_CONTEXT_KEY, JSON.stringify(context))

  const params = new URLSearchParams({
    client_id: clientId, redirect_uri: redirectUri, response_type: 'code',
    scope: 'openid email profile', state, nonce,
    code_challenge: codeChallenge, code_challenge_method: 'S256', prompt: 'select_account',
  })
  return `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`
}

export function consumeGoogleOAuthContext(expectedState: string): OAuthContext {
  const raw = sessionStorage.getItem(OAUTH_CONTEXT_KEY)
  sessionStorage.removeItem(OAUTH_CONTEXT_KEY)
  if (!raw) throw new Error('OAuth session expired. Please try Google sign-in again.')

  let context: OAuthContext
  try { context = JSON.parse(raw) as OAuthContext }
  catch { throw new Error('OAuth session is invalid. Please try Google sign-in again.') }

  if (!context?.state || !context?.codeVerifier || !context?.redirectUri)
    throw new Error('OAuth session is incomplete. Please try again.')
  if (context.state !== expectedState)
    throw new Error('OAuth state mismatch. Please retry sign-in.')

  return context
}

export function clearGoogleOAuthContext(): void {
  sessionStorage.removeItem(OAUTH_CONTEXT_KEY)
}
