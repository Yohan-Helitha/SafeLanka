import { useState } from 'react'
import { useToast } from '@/context/ToastContext'
import { useAssign, useTeams } from '@/hooks/response/useResponse'
import { isApiError } from '@/services'
import type { Assignment } from '@/types'
import { ApiErrorNotice } from '../domain'
import { Button, Dialog, Loading, Segmented } from '../ui'

interface Alt {
  id: string
  name: string
}

/** Mount only while open. Offers available teams; if the chosen one was just taken, shows alternatives. */
export function AssignDialog({ assignment, onClose }: { assignment: Assignment; onClose: () => void }) {
  const { toast } = useToast()
  const teams = useTeams(assignment.districtId)
  const assign = useAssign()
  const [teamId, setTeamId] = useState<string | null>(null)
  const available = (teams.data ?? []).filter((t) => t.status === 'AVAILABLE')
  const alternatives =
    isApiError(assign.error) && assign.error.code === 'TEAM_NOT_AVAILABLE' ? ((assign.error.details?.alternatives as Alt[] | undefined) ?? []) : []

  return (
    <Dialog
      open
      onClose={onClose}
      title="Assign a team"
      description={assignment.task}
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
                },
              )
            }
          >
            Dispatch team
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {teams.isLoading && <Loading />}
        {teams.data && available.length === 0 && <p className="text-muted">No team is available in this district right now.</p>}
        {available.length > 0 && (
          <Segmented
            legend="Available teams"
            columns={1}
            value={teamId}
            onChange={setTeamId}
            options={available.map((t) => ({ value: t.id, label: t.name, description: `${t.teamType.toLowerCase()} team · ${t.capacity} people` }))}
          />
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
      </div>
    </Dialog>
  )
}
