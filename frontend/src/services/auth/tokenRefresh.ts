import { env } from '@/constants/env'
import type { LoginResponse } from '@/types'
import { ApiError } from '../ApiError'
import type { ApiErrorCode } from '../ApiError'
import { setAccessToken } from '../session'

let inFlight: Promise<LoginResponse> | null = null

/**
 * Renews the access token from the refresh cookie.
 *
 * Single-flight: concurrent callers (React StrictMode runs effects twice, several requests can hit
 * a 401 together) share one network call. Refresh tokens rotate, so a second parallel call would
 * present an already-used token and the server would treat it as theft and end the session.
 */
export function refreshAccessToken(): Promise<LoginResponse> {
  inFlight ??= doRefresh().finally(() => {
    inFlight = null
  })
  return inFlight
}

async function doRefresh(): Promise<LoginResponse> {
  let res: Response
  try {
    res = await fetch(`${env.apiBaseUrl.replace(/\/$/, '')}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    })
  } catch {
    throw ApiError.offline()
  }
  if (!res.ok) {
    let code: ApiErrorCode = 'UNAUTHENTICATED'
    let message = 'Log in to continue.'
    try {
      const body = await res.json()
      code = body.error?.code ?? code
      message = body.error?.message ?? message
    } catch {
      /* empty body */
    }
    throw new ApiError(res.status, code, message)
  }
  const login = ((await res.json()) as { data: LoginResponse }).data
  setAccessToken(login.accessToken)
  return login
}
