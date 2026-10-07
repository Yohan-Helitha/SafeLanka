import { useState } from 'react'
import { useToast } from '@/context/ToastContext'
import { useAssign, useTeams } from '@/hooks/response/useResponse'
import { isApiError } from '@/services'
import type { Assignment } from '@/types'
import { ApiErrorNotice } from '../domain'
import { Button, Dialog } from '../ui'

interface Alt {
  id: string
  name: string
}

function classNames(...classes: (string | false | undefined | null)[]) {
  return classes.filter(Boolean).join(' ')
}

export function AssignDialog({ assignment, onClose }: { assignment: Assignment; onClose: () => void }) {
  const { toast } = useToast()
  const teams = useTeams(assignment.districtId)
  const assign = useAssign()
  const [teamId, setTeamId] = useState<string | null>(null)
  const available = (teams.data ?? []).filter((t) => t.status === 'AVAILABLE')
  const alternatives =
    isApiError(assign.error) && assign.error.code === 'TEAM_NOT_AVAILABLE'
      ? ((assign.error.details?.alternatives as Alt[] | undefined) ?? [])
      : []

  const renderRadio = (selected: boolean) => {
    if (selected) {
      return (
        <div className="flex items-center justify-center" aria-hidden>
          <div className="h-5 w-5 rounded-full bg-command-accent flex items-center justify-center">
            <div className="h-2.5 w-2.5 rounded-full bg-white" />
          </div>
        </div>
      )
    }
    return (
      <div className="flex items-center justify-center" aria-hidden>
        <div className="h-5 w-5 rounded-full border border-command-borderLight bg-transparent" />
      </div>
    )
  }

  const selectedBorderClass = 'border-2 border-cyan-400'
  const selectedShadowClass = 'shadow-[0_0_18px_rgba(34,211,238,0.18)]'

  return (
    <Dialog
      open
      onClose={onClose}
      title="Assign a team"
      description={assignment.task}
      className="max-w-xl"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            disabled={!teamId}
            loading={assign.isPending}
            onClick={() =>
              assign.mutate(
                { id: assignment.id, teamId: teamId! },
                {
                  onSuccess: () => {
                    toast('Team dispatched')
                    onClose()
                  },
                }
              )
            }
          >
            Dispatch team
          </Button>
        </>
      }
    >
      <fieldset className="space-y-4">
        <legend className="text-xs font-semibold uppercase tracking-wider text-slate-400">
          Available teams
        </legend>
        {teams.isLoading && <p className="text-sm text-muted">Loading available teams...</p>}
        {teams.data && available.length === 0 && (
          <p className="text-sm text-muted">No team is available in this district right now.</p>
        )}
        {available.length > 0 && (
          <div className="space-y-3" role="radiogroup" aria-label="Available teams">
            {available.map((t) => {
              const selected = teamId === t.id
              return (
                 <label
                  key={t.id}
                  className={classNames(
                    'flex cursor-pointer items-center justify-between rounded-lg p-4 transition-all duration-150 select-none',
                     selected
                       ? `${selectedBorderClass} ${selectedShadowClass}`
                       : 'border border-command-border/70 bg-command-card hover:border-command-borderLight hover:bg-command-card/80'
                  )}
                >
                  <div className="space-y-0.5">
                    <span className={classNames('block text-sm', selected ? 'font-semibold text-white' : 'font-medium text-slate-200')}>
                      {t.name}
                    </span>
                    <span className="block text-xs text-command-textMuted">
                      {t.teamType.toLowerCase()} team · {t.capacity} people
                    </span>
                  </div>
                  <input
                    className="sr-only"
                    name="team_selection"
                    type="radio"
                    value={t.id}
                    checked={selected}
                    onChange={() => setTeamId(t.id)}
                  />
                  {renderRadio(selected)}
                </label>
              )
            })}
          </div>
        )}
        <ApiErrorNotice error={assign.error}>
          {alternatives.length > 0 && (
            <ul className="mt-1 space-y-1">
              {alternatives.map((a) => (
                <li key={a.id}>
                  <button type="button" className="text-signal hover:underline" onClick={() => setTeamId(a.id)}>
                    Use {a.name}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </ApiErrorNotice>
      </fieldset>
    </Dialog>
  )
}
