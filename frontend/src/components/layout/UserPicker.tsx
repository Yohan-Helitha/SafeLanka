import { useQuery } from '@tanstack/react-query'
import { ROLE_LABEL, ROLE_ORDER } from '@/constants/roles'
import { useAuth } from '@/context/AuthContext'
import { useReferenceData } from '@/hooks/shared'
import { api } from '@/services'
import type { AppUser } from '@/types'
import { ErrorState, Loading } from '../ui'

interface Props {
  onPick: (user: AppUser) => void
}

/** Named seed users grouped by role (demo residents are left out). */
export function UserPicker({ onPick }: Props) {
  const { user: current } = useAuth()
  const { districtName } = useReferenceData()
  const users = useQuery({
    queryKey: ['reference', 'users'],
    queryFn: () => api.reference.users(),
    staleTime: 10 * 60_000,
  })

  if (users.isLoading) return <Loading label="Loading people" />
  if (users.isError) return <ErrorState error={users.error} onRetry={() => void users.refetch()} />

  const named = (users.data ?? []).filter((u) => u.named)
  return (
    <div className="space-y-5">
      {ROLE_ORDER.map((role) => {
        const group = named.filter((u) => u.role === role)
        if (!group.length) return null
        return (
          <section key={role} aria-labelledby={`role-${role}`}>
            <h3 id={`role-${role}`} className="mb-1.5 text-xs font-medium uppercase tracking-wide text-faint">
              {ROLE_LABEL[role]}
            </h3>
            <ul className="space-y-1.5">
              {group.map((u) => (
                <li key={u.id}>
                  <button
                    type="button"
                    onClick={() => onPick(u)}
                    aria-current={current?.id === u.id ? 'true' : undefined}
                    className="flex min-h-[52px] w-full items-center justify-between gap-3 rounded-control border border-line bg-raised px-4 py-2 text-left transition-colors hover:border-signal/60 aria-[current=true]:border-signal"
                  >
                    <span className="font-medium text-ink">{u.fullName}</span>
                    <span className="text-sm text-muted">{districtName(u.districtId)}</span>
                  </button>
                </li>
              ))}
            </ul>
          </section>
        )
      })}
    </div>
  )
}
