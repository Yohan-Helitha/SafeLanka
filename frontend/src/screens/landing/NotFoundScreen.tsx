import { Compass } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui'
import { homeFor } from '@/constants/roles'
import { entryPath } from '@/constants/routes'
import { useAuth } from '@/context/AuthContext'
import { useDocumentTitle } from '@/hooks/shared'

export function NotFoundScreen() {
  useDocumentTitle('Page not found')
  const { user } = useAuth()
  return (
    <div data-surface="portal" className="grid min-h-screen place-items-center bg-canvas px-4 text-center text-ink">
      <div>
        <Compass className="mx-auto size-12 text-faint" aria-hidden />
        <h1 className="mt-3 font-display text-3xl font-semibold">Page not found</h1>
        <p className="mt-1 text-muted">The page you opened does not exist or has moved.</p>
        <Link to={user ? homeFor(user.role) : entryPath} className="mt-5 inline-block">
          <Button>{user ? 'Go to your home screen' : 'Go to login'}</Button>
        </Link>
      </div>
    </div>
  )
}
