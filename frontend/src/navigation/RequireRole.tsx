import { Navigate, Outlet } from 'react-router-dom'
import { Loading } from '@/components/ui'
import { homeFor } from '@/constants/roles'
import { paths } from '@/constants/routes'
import { useActingUser } from '@/context/ActingUserContext'
import type { Role } from '@/types'

/** No person chosen -> landing. Wrong role -> that role's own home. */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { user, loading } = useActingUser()
  if (loading) return <Loading />
  if (!user) return <Navigate to={paths.landing} replace />
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />
  return <Outlet />
}
