import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api } from '@/services'
import { getActingUserId, setActingUserId } from '@/services/session'
import type { AppUser } from '@/types'

interface ActingUserState {
  user: AppUser | null
  /** True while the saved user is still being looked up. */
  loading: boolean
  signIn: (user: AppUser) => void
  signOut: () => void
}

const ActingUserContext = createContext<ActingUserState | null>(null)

export function ActingUserProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [userId, setUserId] = useState<string | null>(() => getActingUserId())

  const users = useQuery({
    queryKey: ['reference', 'users'],
    queryFn: () => api.reference.users(),
    staleTime: 10 * 60_000,
  })

  const user = useMemo(() => users.data?.find((u) => u.id === userId) ?? null, [users.data, userId])

  const signIn = useCallback(
    (next: AppUser) => {
      setActingUserId(next.id)
      setUserId(next.id)
      // Everything cached belongs to the previous person.
      queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== 'reference' })
    },
    [queryClient],
  )

  const signOut = useCallback(() => {
    setActingUserId(null)
    setUserId(null)
    queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== 'reference' })
  }, [queryClient])

  const value = useMemo(
    () => ({ user, loading: Boolean(userId) && users.isLoading, signIn, signOut }),
    [user, userId, users.isLoading, signIn, signOut],
  )

  return <ActingUserContext.Provider value={value}>{children}</ActingUserContext.Provider>
}

export function useActingUser(): ActingUserState {
  const ctx = useContext(ActingUserContext)
  if (!ctx) throw new Error('useActingUser must be used inside ActingUserProvider')
  return ctx
}

/** For screens behind RequireRole, where a user is guaranteed. */
export function useCurrentUser(): AppUser {
  const { user } = useActingUser()
  if (!user) throw new Error('useCurrentUser needs a signed-in user')
  return user
}
