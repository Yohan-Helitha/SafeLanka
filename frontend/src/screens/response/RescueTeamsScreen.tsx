import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiErrorNotice, StatusChip } from '@/components/domain'
import { AssignDialog } from '@/components/response/AssignDialog'
import { Button, Card, DataTable, ErrorState, Loading, PageHeader } from '@/components/ui'
import { PRIORITY_LABEL, TEAM_TYPE_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/ActingUserContext'
import { useToast } from '@/context/ToastContext'
import { useDocumentTitle } from '@/hooks/shared'
import { useAssignments, useCancelAssignment, useTeams } from '@/hooks/response/useResponse'
import type { Assignment, RescueTeam } from '@/types'
import { relativeTime } from '@/utils/format'

export function RescueTeamsScreen() {
  useDocumentTitle('Rescue teams')
  const user = useCurrentUser()
  const navigate = useNavigate()
  const { toast } = useToast()
  const teams = useTeams(user.districtId)
  const assignments = useAssignments(user.districtId)
  const cancel = useCancelAssignment()
  const [assigning, setAssigning] = useState<Assignment | null>(null)

  return (
    <div>
      <PageHeader
        title="Rescue teams"
        subtitle="See who is available, and send teams where they are needed."
        actions={
          <Button icon={<Plus className="size-4" aria-hidden />} onClick={() => navigate(paths.district.newAssignment)}>
            New assignment
          </Button>
        }
      />
      <div className="space-y-5">
        <Card title="Teams" padded={false}>
          <div className="p-4 pt-3">
            {teams.isLoading && <Loading />}
            {teams.isError && <ErrorState error={teams.error} onRetry={() => void teams.refetch()} />}
            {teams.data && (
              <DataTable<RescueTeam>
                caption="Rescue teams in this district"
                rows={teams.data}
                rowKey={(t) => t.id}
                columns={[
                  { key: 'name', header: 'Team', cell: (t) => <span className="font-medium">{t.name}</span> },
                  { key: 'type', header: 'Type', cell: (t) => TEAM_TYPE_LABEL[t.teamType] },
                  { key: 'cap', header: 'Capacity', cell: (t) => <span className="tabular">{t.capacity} people</span> },
                  { key: 'status', header: 'Status', cell: (t) => <StatusChip status={t.status} /> },
                  { key: 'last', header: 'Last update', cell: (t) => <span className="text-muted">{relativeTime(t.lastStatusAt)}</span> },
                ]}
              />
            )}
          </div>
        </Card>

        <Card title="Assignments" padded={false}>
          <div className="space-y-3 p-4 pt-3">
            <ApiErrorNotice error={cancel.error} />
            {assignments.isLoading && <Loading />}
            {assignments.isError && <ErrorState error={assignments.error} onRetry={() => void assignments.refetch()} />}
            {assignments.data && (
              <DataTable<Assignment>
                caption="Assignments in this district, newest first"
                rows={assignments.data}
                rowKey={(a) => a.id}
                minWidth={820}
                empty={<p className="py-6 text-center text-muted">No assignments yet.</p>}
                columns={[
                  {
                    key: 'task',
                    header: 'Task',
                    cell: (a) => (
                      <span>
                        <span className="block font-medium">{a.task}</span>
                        <span className="text-sm text-muted">{a.locationText}</span>
                      </span>
                    ),
                  },
                  { key: 'priority', header: 'Priority', cell: (a) => <span className={a.priority === 1 ? 'font-medium text-danger' : ''}>{PRIORITY_LABEL[a.priority]}</span> },
                  { key: 'team', header: 'Team', cell: (a) => a.teamName ?? <span className="text-faint">Not assigned</span> },
                  { key: 'status', header: 'Status', cell: (a) => <StatusChip status={a.status} /> },
                  {
                    key: 'actions',
                    header: '',
                    align: 'right',
                    cell: (a) => (
                      <span className="flex justify-end gap-2">
                        {a.status === 'UNASSIGNED' && (
                          <Button size="sm" onClick={() => setAssigning(a)}>
                            Assign team
                          </Button>
                        )}
                        {a.status !== 'COMPLETED' && a.status !== 'CANCELLED' && (
                          <Button size="sm" variant="ghost" loading={cancel.isPending && cancel.variables === a.id} onClick={() => cancel.mutate(a.id, { onSuccess: () => toast('Assignment cancelled') })}>
                            Cancel
                          </Button>
                        )}
                      </span>
                    ),
                  },
                ]}
              />
            )}
          </div>
        </Card>
      </div>
      {assigning && <AssignDialog assignment={assigning} onClose={() => setAssigning(null)} />}
    </div>
  )
}
