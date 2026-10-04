import { LogOut, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROLE_LABEL } from '@/constants/roles'
import { entryPath } from '@/constants/routes'
import { useAuth } from '@/context/AuthContext'
import { useReferenceData } from '@/hooks/shared'
import { Button, Dialog } from '../ui'

interface Props {
  compact?: boolean
}

/** Shows who is signed in and offers Log out. Used in login mode instead of the role switcher. */
export function AccountMenu({ compact = false }: Props) {
  const { user, signOut } = useAuth()
  const { districtName } = useReferenceData()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const logOut = async () => {
    setBusy(true)
    await signOut()
    navigate(entryPath, { replace: true })
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Account"
        className="flex min-h-9 max-w-full items-center gap-2 rounded-control border border-line px-2.5 text-left text-sm text-ink hover:border-signal/60"
      >
        <UserRound className="size-4 shrink-0 text-signal" aria-hidden />
        {!compact && user && (
          <span className="min-w-0">
            <span className="block truncate font-medium leading-tight">{user.fullName}</span>
            <span className="block truncate text-xs leading-tight text-muted">{ROLE_LABEL[user.role]}</span>
          </span>
        )}
      </button>
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="Your account"
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Close
            </Button>
            <Button variant="danger" loading={busy} icon={<LogOut className="size-4" aria-hidden />} onClick={() => void logOut()}>
              Log out
            </Button>
          </>
        }
      >
        {user && (
          <dl className="space-y-2 text-[15px]">
            <div>
              <dt className="text-sm text-muted">Name</dt>
              <dd className="font-medium text-ink">{user.fullName}</dd>
            </div>
            <div>
              <dt className="text-sm text-muted">Role</dt>
              <dd className="text-ink">{ROLE_LABEL[user.role]}</dd>
            </div>
            <div>
              <dt className="text-sm text-muted">District</dt>
              <dd className="text-ink">{districtName(user.districtId)}</dd>
            </div>
          </dl>
        )}
      </Dialog>
    </>
  )
}
