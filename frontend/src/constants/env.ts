const raw = import.meta.env

export type AuthMode = 'login' | 'demo'

export const env = {
  /** true = in-browser mock database, false = Spring Boot API. */
  useMocks: String(raw.VITE_USE_MOCKS ?? 'true') !== 'false',
  /** Relative by default: the Vite dev server proxies /api, which keeps the refresh cookie same-origin. */
  apiBaseUrl: String(raw.VITE_API_BASE_URL ?? '/api'),
  /**
   * login = real accounts (mobile or email + password).
   * demo  = the role switcher, for a safe demo if login breaks (backend profile demo-auth).
   */
  authMode: (String(raw.VITE_AUTH_MODE ?? 'login') === 'demo' ? 'demo' : 'login') as AuthMode,
} as const
