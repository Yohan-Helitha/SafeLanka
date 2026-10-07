import { LogOut, Shield, MapPin, UserRound } from 'lucide-react'
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

function getInitials(name: string) {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
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
        className="max-w-[540px]"
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
          <div className="space-y-5">
            {/* Tactical Top Glow Accent Line */}
            <div className="h-1 w-full bg-gradient-to-r from-transparent via-cyan-500 to-transparent opacity-80 -mx-5 -mt-4 mb-5" />

            {/* Session Badge */}
            <div className="flex items-center justify-between px-3.5 py-2 rounded-lg bg-raised border border-line text-xs">
              <div className="flex items-center gap-2">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
                </span>
                <span className="text-ink/80 font-medium">SafeLanka Incident Command</span>
              </div>
              <span className="text-signal font-mono tracking-wider font-semibold uppercase text-[11px] bg-cyan-500/10 px-2 py-0.5 rounded border border-cyan-500/20">
                Active Session
              </span>
            </div>

            {/* Officer Identity Card */}
            <section className="p-4 rounded-xl bg-raised border border-line flex items-center gap-4">
              <div className="relative shrink-0">
                <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-surface to-[#1e273a] border border-signal/30 flex items-center justify-center text-signal font-bold text-lg tracking-wider shadow-inner shadow-cyan-500/10">
                  {getInitials(user.fullName)}
                </div>
                <span
                  className="absolute -bottom-1 -right-1 block h-3.5 w-3.5 rounded-full bg-emerald-500 border-2 border-raised ring-1 ring-emerald-400"
                  title="Active on duty"
                />
              </div>
              <div className="space-y-0.5 min-w-0">
                <p className="text-xs font-medium text-muted uppercase tracking-wider">Name</p>
                <p className="text-base font-semibold text-ink tracking-tight truncate">{user.fullName}</p>
                <p className="text-xs text-muted flex items-center gap-1.5 pt-0.5">
                  <Shield className="w-3.5 h-3.5 text-signal shrink-0" aria-hidden />
                  Command Dispatcher ID: #{user.id?.slice(0, 6).toUpperCase()}
                </p>
              </div>
            </section>

            {/* Role and District Grid */}
            <section className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Role Block */}
              <div className="p-3.5 rounded-xl bg-surface border border-line space-y-1.5 transition-colors hover:border-signal/30">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted uppercase tracking-wider">Role</span>
                  <Shield className="w-4 h-4 text-signal" aria-hidden />
                </div>
                <p className="text-sm font-semibold text-ink flex items-center gap-1.5">
                  {ROLE_LABEL[user.role]}
                </p>
              </div>

              {/* District Block */}
              <div className="p-3.5 rounded-xl bg-surface border border-line space-y-1.5 transition-colors hover:border-signal/30">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted uppercase tracking-wider">District</span>
                  <MapPin className="w-4 h-4 text-signal" aria-hidden />
                </div>
                <p className="text-sm font-semibold text-ink flex items-center gap-1.5">
                  {districtName(user.districtId)}
                  <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-raised text-muted border border-line">Western Prov.</span>
                </p>
              </div>
            </section>
          </div>
        )}
      </Dialog>
    </>
  )
}
