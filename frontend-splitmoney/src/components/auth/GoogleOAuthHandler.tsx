import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { loginWithGoogleCode } from '../../api/auth'
import { clearGoogleOAuthContext, consumeGoogleOAuthContext } from '../../utils/oauthGoogle'

const LOCK_PREFIX = 'splitwise_google_oauth_exchange_lock:'

function cleanOAuthParams() {
  const url = new URL(window.location.href)
  ;['code', 'state', 'scope', 'authuser', 'prompt', 'error', 'error_description']
    .forEach(k => url.searchParams.delete(k))
  window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`)
}

/**
 * Silently handles the Google OAuth redirect callback.
 * Reads ?code and ?state from the URL, exchanges them with the backend,
 * then cleans up the URL params. Renders nothing.
 */
export function GoogleOAuthHandler() {
  const { applySession } = useAuth()
  const navigate = useNavigate()
  const handledRef = useRef(false)

  useEffect(() => {
    const url = new URL(window.location.href)
    const code = url.searchParams.get('code')
    const state = url.searchParams.get('state')
    const oauthError = url.searchParams.get('error')
    const oauthErrorDesc = url.searchParams.get('error_description')

    if (!code && !oauthError) return
    if (handledRef.current) return
    handledRef.current = true

    if (oauthError) {
      clearGoogleOAuthContext()
      cleanOAuthParams()
      navigate('/signin', {
        replace: true,
        state: { error: `Google sign-in failed: ${oauthErrorDesc ?? oauthError}` },
      })
      return
    }

    if (!state) {
      clearGoogleOAuthContext()
      cleanOAuthParams()
      navigate('/signin', {
        replace: true,
        state: { error: 'Missing OAuth state. Please retry Google sign-in.' },
      })
      return
    }

    const lockKey = `${LOCK_PREFIX}${state}:${code}`
    if (sessionStorage.getItem(lockKey)) return
    sessionStorage.setItem(lockKey, '1')

    ;(async () => {
      try {
        const context = consumeGoogleOAuthContext(state)
        const result = await loginWithGoogleCode({
          authorizationCode: code!,
          redirectUri: context.redirectUri as string,
          codeVerifier: context.codeVerifier as string,
          nonce: context.nonce as string,
        })
        if (result.ok && result.body?.success && result.body?.data) {
          applySession(result.body.data as Parameters<typeof applySession>[0])
          navigate('/dashboard', { replace: true })
        } else {
          const msg = result.body?.responseMessage ?? 'Unable to log in with Google.'
          navigate('/signin', { replace: true, state: { error: msg } })
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Google login failed.'
        navigate('/signin', { replace: true, state: { error: msg } })
      } finally {
        cleanOAuthParams()
        sessionStorage.removeItem(lockKey)
      }
    })()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return null
}
