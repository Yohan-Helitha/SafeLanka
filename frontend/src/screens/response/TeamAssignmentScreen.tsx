import { Check, ClipboardCheck } from 'lucide-react'
import { useState } from 'react'
import { ApiErrorNotice } from '@/components/domain'
import { AssignmentSummary } from '@/components/response/AssignmentSummary'
import { Button, Dialog, EmptyState, ErrorState, Field, Loading, PageHeader, TextArea } from '@/components/ui'
import { useToast } from '@/context/ToastContext'
import { useCurrentUser } from '@/context/ActingUserContext'
import { useDocumentTitle, useOnlineStatus } from '@/hooks/shared'
import { useMyAssignment, useRespond, useUpdateTeamStatus } from '@/hooks/response/useResponse'
import type { AssignmentStatus, TeamStatus } from '@/types'
import { newClientRef } from '@/utils/id'

const ORDER: AssignmentStatus[] = ['PENDING_ACK', 'ACCEPTED', 'EN_ROUTE', 'ACTIVE', 'COMPLETED']

const STEPS: { status: AssignmentStatus; team: TeamStatus; button: string; done: string; saved: string }[] = [
  { status: 'EN_ROUTE', team: 'EN_ROUTE', button: "We're on our way", done: 'On the way', saved: 'En route' },
  { status: 'ACTIVE', team: 'ACTIVE', button: "We've arrived", done: 'Arrived', saved: 'Arrival' },
  { status: 'COMPLETED', team: 'AVAILABLE', button: 'Task complete', done: 'Task complete', saved: 'Completion' },
]

export function TeamAssignmentScreen() {
  useDocumentTitle('Assignment')
  const user = useCurrentUser()
  const { toast } = useToast()
  const online = useOnlineStatus()
  const mine = useMyAssignment()
  const respond = useRespond()
  const status = useUpdateTeamStatus()
  const [declining, setDeclining] = useState(false)
  const [reason, setReason] = useState('')
  // Steps saved offline move the screen forward before the server confirms them.
  const [local, setLocal] = useState<{ id: string; status: AssignmentStatus } | null>(null)

  if (mine.isLoading) return <Loading />
  if (mine.isError) return <ErrorState error={mine.error} onRetry={() => void mine.refetch()} />
  const a = mine.data
  if (!a) {
    return (
      <div>
        <PageHeader title="Assignment" />
        <EmptyState
          icon={ClipboardCheck}
          title="No assignment right now"
          description="Stay ready. New assignments from the district officer appear here and need your answer."
        />
      </div>
    )
  }

  const serverRank = ORDER.indexOf(a.status)
  const effective = local && local.id === a.id && ORDER.indexOf(local.status) > serverRank ? local.status : a.status
  const rank = ORDER.indexOf(effective)

  const advance = (step: (typeof STEPS)[number]) =>
    status.mutate(
      {
        teamId: user.rescueTeamId!,
        label: `${step.saved} update`,
        update: { toStatus: step.team, clientRef: newClientRef(), changedAt: new Date().toISOString(), recordedOffline: !online },
      },
      {
        onSuccess: (r) => {
          if (r.queued) {
            setLocal({ id: a.id, status: step.status })
            toast(`${step.saved} saved on this phone; it will send when you're back online`)
          } else toast(`${step.saved === 'En route' ? 'En route' : step.saved} recorded`)
        },
      },
    )

  const nextStep = STEPS.find((s) => ORDER.indexOf(s.status) === rank + 1)

  return (
    <div>
      <PageHeader title="Assignment" subtitle={a.teamName ?? undefined} />
      <AssignmentSummary
        assignment={a}
        status={effective}
        footer={
          effective === 'PENDING_ACK' ? (
            <div className="space-y-3">
              <ApiErrorNotice error={respond.error} />
              <div className="grid grid-cols-2 gap-3">
                <Button variant="secondary" size="lg" onClick={() => setDeclining(true)}>
                  Decline
                </Button>
                <Button size="lg" loading={respond.isPending} onClick={() => respond.mutate({ id: a.id, accept: true }, { onSuccess: () => toast('Assignment accepted') })}>
                  Accept
                </Button>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <ol className="space-y-2">
                {STEPS.map((s) => {
                  const done = rank >= ORDER.indexOf(s.status)
                  return (
                    <li key={s.status} className={`flex items-center gap-3 rounded-control px-3 py-2.5 ${done ? 'bg-ok/10 text-ok' : 'text-faint'}`}>
                      {done ? <Check className="size-5" aria-hidden /> : <span className="size-5 rounded-full border-2 border-current" aria-hidden />}
                      <span className="font-medium">{s.done}</span>
                    </li>
                  )
                })}
              </ol>
              <ApiErrorNotice error={status.error} />
              {nextStep ? (
                <Button size="lg" block loading={status.isPending} onClick={() => advance(nextStep)}>
                  {nextStep.button}
                </Button>
              ) : (
                <p className="text-center text-sm text-muted">Well done. Your team is available for the next assignment.</p>
              )}
            </div>
          )
        }
      />

      <Dialog
        open={declining}
        onClose={() => setDeclining(false)}
        title="Decline assignment"
        description="The district officer is told and will send another team."
        footer={
          <>
            <Button variant="ghost" onClick={() => setDeclining(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              loading={respond.isPending}
              disabled={reason.trim().length < 5}
              onClick={() =>
                respond.mutate(
                  { id: a.id, accept: false, declineReason: reason.trim() },
                  {
                    onSuccess: () => {
                      toast('Assignment declined')
                      setDeclining(false)
                      setReason('')
                    },
                  },
                )
              }
            >
              Decline assignment
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <Field label="Reason" required hint="At least 5 characters.">
            {(p) => <TextArea {...p} rows={3} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Boat engine failed, team is repairing it." />}
          </Field>
          <ApiErrorNotice error={respond.error} />
        </div>
      </Dialog>
    </div>
  )
}
