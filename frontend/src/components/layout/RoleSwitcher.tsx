import { UserRound } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { homeFor, ROLE_LABEL } from '@/constants/roles'
import { paths } from '@/constants/routes'
import { useAuth } from '@/context/AuthContext'
import type { AppUser } from '@/types'
import { Button, Dialog } from '../ui'
import { UserPicker } from './UserPicker'

interface Props {
  compact?: boolean
}

export function RoleSwitcher({ compact = false }: Props) {
  const { user, signIn, signOut } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const pick = (next: AppUser) => {
    signIn(next)
    setOpen(false)
    navigate(homeFor(next.role))
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="flex min-h-9 max-w-full items-center gap-2 rounded-control border border-line px-2.5 text-left text-sm text-ink hover:border-signal/60"
        aria-label="Switch person"
      >
        <UserRound className="size-4 shrink-0 text-signal" aria-hidden />
        {!compact && (
          <span className="min-w-0">
            <span className="block truncate font-medium leading-tight">{user?.fullName ?? 'Choose a person'}</span>
            {user && <span className="block truncate text-xs leading-tight text-muted">{ROLE_LABEL[user.role]}</span>}
          </span>
        )}
      </button>
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="Act as someone else"
        description="Pick a person to see the app from their role. There is no login in this demo."
        footer={
          <Button
            variant="ghost"
            onClick={() => {
              signOut()
              setOpen(false)
              navigate(paths.landing)
            }}
          >
            Back to start
          </Button>
        }
      >
        <UserPicker onPick={pick} />
      </Dialog>
    </>
  )
}
