import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { env } from '@/constants/env'
import type { AuthMode } from '@/constants/env'
import { api } from '@/services'
import { getActingUserId, onSessionExpired, setAccessToken, setActingUserId } from '@/services/session'
import type { LoginResponse, SessionUser } from '@/types'

interface AuthState {
  user: SessionUser | null
  /** True while the saved session is being restored (page load). */
  loading: boolean
  mode: AuthMode
  /** Login mode: record a successful login, signup verification or refresh. */
  applyLogin: (login: LoginResponse) => void
  /** Demo mode: act as the chosen person. */
  signIn: (user: SessionUser) => void
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

const NOT_REFERENCE = (q: { queryKey: readonly unknown[] }) => q.queryKey[0] !== 'reference'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const demo = env.authMode === 'demo'

  // Login mode: the user comes from the session (restored through the refresh cookie on load).
  const [session, setSession] = useState<{ user: SessionUser | null; loading: boolean }>({ user: null, loading: !demo })

  // Demo mode: the user is looked up from the seeded list by the saved id.
  const [demoId, setDemoId] = useState<string | null>(() => getActingUserId())
  const users = useQuery({
    queryKey: ['reference', 'users'],
    queryFn: () => api.reference.users(),
    staleTime: 10 * 60_000,
    enabled: demo,
  })

  useEffect(() => {
    if (demo) return
    let alive = true
    api.auth
      .refresh()
      .then((login) => alive && setSession({ user: login.user, loading: false }))
      .catch(() => alive && setSession({ user: null, loading: false }))
    return () => {
      alive = false
    }
  }, [demo])

  // A session that could not be renewed mid-use sends the person back to the login screen.
  useEffect(
    () =>
      onSessionExpired(() => {
        setSession({ user: null, loading: false })
        queryClient.removeQueries({ predicate: NOT_REFERENCE })
      }),
    [queryClient],
  )

  const applyLogin = useCallback(
    (login: LoginResponse) => {
      // Everything cached belongs to whoever was signed in before.
      queryClient.removeQueries({ predicate: NOT_REFERENCE })
      setSession({ user: login.user, loading: false })
    },
    [queryClient],
  )

  const signIn = useCallback(
    (next: SessionUser) => {
      setActingUserId(next.id)
      setDemoId(next.id)
      queryClient.removeQueries({ predicate: NOT_REFERENCE })
    },
    [queryClient],
  )

  const signOut = useCallback(async () => {
    try {
      if (demo) {
        setActingUserId(null)
        setDemoId(null)
      } else {
        await api.auth.logout()
      }
    } catch {
      /* the server session is gone or unreachable: sign out locally regardless */
    } finally {
      setAccessToken(null)
      setSession({ user: null, loading: false })
      queryClient.removeQueries({ predicate: NOT_REFERENCE })
    }
  }, [demo, queryClient])

  const value = useMemo<AuthState>(() => {
    const user = demo ? (users.data?.find((u) => u.id === demoId) ?? null) : session.user
    const loading = demo ? Boolean(demoId) && users.isLoading : session.loading
    return { user, loading, mode: env.authMode, applyLogin, signIn, signOut }
  }, [demo, users.data, users.isLoading, demoId, session, applyLogin, signIn, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

/** For screens behind RequireRole, where a user is guaranteed. */
export function useCurrentUser(): SessionUser {
  const { user } = useAuth()
  if (!user) throw new Error('useCurrentUser needs a signed-in user')
  return user
}
