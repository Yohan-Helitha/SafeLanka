import { ShieldCheck } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { UserPicker } from '@/components/layout/UserPicker'
import { APP_NAME } from '@/constants/app'
import { homeFor, ROLE_LABEL } from '@/constants/roles'
import { useActingUser } from '@/context/ActingUserContext'
import { useDocumentTitle } from '@/hooks/shared'
import type { AppUser } from '@/types'

/** Entry point: there is no login, so people choose who to act as. */
export function RoleSelectScreen() {
  useDocumentTitle('')
  const { user, signIn } = useActingUser()
  const navigate = useNavigate()

  const pick = (next: AppUser) => {
    signIn(next)
    navigate(homeFor(next.role))
  }

  return (
    <div data-surface="portal" className="min-h-screen bg-canvas px-4 py-10 text-ink">
      <main className="mx-auto max-w-xl">
        <div className="mb-8 text-center">
          <ShieldCheck className="mx-auto size-12 text-signal" aria-hidden />
          <h1 className="mt-3 font-display text-4xl font-semibold">{APP_NAME}</h1>
          <p className="mt-1 text-muted">Smart early warning and emergency coordination for Sri Lanka</p>
        </div>
        <div className="rounded-card border border-line bg-panel p-5">
          <h2 className="font-display text-xl font-semibold">Choose who to act as</h2>
          <p className="mb-4 mt-1 text-sm text-muted">There is no login in this demo. Pick a person to see the system from their role.</p>
          {user && (
            <p className="mb-4 rounded-control border border-line bg-raised px-3 py-2 text-sm">
              Signed in as <span className="font-medium">{user.fullName}</span> ({ROLE_LABEL[user.role]}).{' '}
              <Link to={homeFor(user.role)} className="text-signal hover:underline">
                Continue
              </Link>
            </p>
          )}
          <UserPicker onPick={pick} />
        </div>
      </main>
    </div>
  )
}
