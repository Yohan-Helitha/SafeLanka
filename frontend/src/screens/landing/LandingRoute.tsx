import { Navigate } from 'react-router-dom'
import { Loading } from '@/components/ui'
import { homeFor } from '@/constants/roles'
import { paths } from '@/constants/routes'
import { useAuth } from '@/context/AuthContext'
import { RoleSelectScreen } from './RoleSelectScreen'

/** "/" is the role picker in demo mode; with real login it sends people to their home or the login screen. */
export function LandingRoute() {
  const { user, loading, mode } = useAuth()
  if (mode === 'demo') return <RoleSelectScreen />
  if (loading) return <Loading />
  return <Navigate to={user ? homeFor(user.role) : paths.auth.login} replace />
}
