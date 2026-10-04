import { Navigate, Outlet } from 'react-router-dom'
import { Loading } from '@/components/ui'
import { homeFor } from '@/constants/roles'
import { entryPath } from '@/constants/routes'
import { useAuth } from '@/context/AuthContext'
import type { Role } from '@/types'

/** Not signed in -> login (or the role picker in demo mode). Wrong role -> that role's own home. */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { user, loading } = useAuth()
  if (loading) return <Loading />
  if (!user) return <Navigate to={entryPath} replace />
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />
  return <Outlet />
}
