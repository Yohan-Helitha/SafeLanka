import type { LoginResponse } from '@/types'
import { http } from '../http'
import { setAccessToken } from '../session'
import type { AuthApi } from './authApi'
import { refreshAccessToken } from './tokenRefresh'

/** Keeps the new access token in memory; the refresh token is already in its httpOnly cookie. */
function established(login: LoginResponse): LoginResponse {
  setAccessToken(login.accessToken)
  return login
}

export const authHttp: AuthApi = {
  signup: (input) => http.post('/auth/signup', input),
  verifyPhone: async (phone, code) => established(await http.post('/auth/verify-phone', { phone, code })),
  resendCode: (phone) => http.post('/auth/resend-code', { phone }),
  login: async (input) => established(await http.post('/auth/login', input)),
  refresh: () => refreshAccessToken(),
  async logout() {
    try {
      await http.post('/auth/logout')
    } finally {
      setAccessToken(null)
    }
  },
  me: () => http.get('/auth/me'),
}
