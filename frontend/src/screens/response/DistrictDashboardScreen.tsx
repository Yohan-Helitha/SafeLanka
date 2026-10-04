import { Link } from 'react-router-dom'
import { StatusChip } from '@/components/domain'
import { Button, Card, ErrorState, Loading, PageHeader, Stat } from '@/components/ui'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useDashboard } from '@/hooks/response/useResponse'
import type { TeamStatus } from '@/types'
import { formatPercent, relativeTime } from '@/utils/format'

const TEAM_ORDER: TeamStatus[] = ['AVAILABLE', 'DISPATCHED', 'EN_ROUTE', 'ACTIVE', 'OFFLINE_UNKNOWN']

export function DistrictDashboardScreen() {
  useDocumentTitle('Dashboard')
  const user = useCurrentUser()
  const { districtName, eventName } = useReferenceData()
  const dashboard = useDashboard(user.districtId)

  if (dashboard.isLoading) return <Loading />
  if (dashboard.isError || !dashboard.data) return <ErrorState error={dashboard.error} onRetry={() => void dashboard.refetch()} />
  const d = dashboard.data
  const ratio = d.shelterCapacity ? d.sheltersOccupied / d.shelterCapacity : 0

  return (
    <div>
      <PageHeader
        title={`${districtName(user.districtId)} district`}
        subtitle={d.activeEventId ? `Active event: ${eventName(d.activeEventId)}` : 'No active disaster event in this district.'}
        actions={
          <Link to={paths.district.newAssignment}>
            <Button>New assignment</Button>
          </Link>
        }
      />

      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Teams available" value={d.teamsByStatus.AVAILABLE} tone="signal" />
        <Stat label="Open assignments" value={d.openAssignments} hint={d.pendingAcknowledgement ? `${d.pendingAcknowledgement} waiting for an answer` : undefined} tone={d.pendingAcknowledgement ? 'caution' : 'default'} />
        <Stat label="Shelter occupancy" value={formatPercent(ratio)} hint={`${d.sheltersOccupied} of ${d.shelterCapacity} places`} tone={d.nearlyFullShelters ? 'caution' : 'default'} />
        <Stat label="Active warnings" value={d.activeWarnings} tone={d.activeWarnings ? 'danger' : 'default'} />
      </div>

      <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <Card title="Recent disaster updates & actions">
          {d.activity.length === 0 ? (
            <p className="text-sm text-muted">Nothing has happened in this district yet.</p>
          ) : (
            <ol className="space-y-3">
              {d.activity.map((e) => (
                <li key={e.id} className="flex gap-3">
                  <span className="mt-2 size-2 shrink-0 rounded-full bg-signal" aria-hidden />
                  <div>
                    <p className="text-[15px] text-ink">{e.message}</p>
                    <p className="text-xs text-faint">{relativeTime(e.occurredAt)}</p>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </Card>
        <div className="space-y-4">
          <Card title="Rescue teams" actions={<Link to={paths.district.teams} className="text-sm text-signal hover:underline">View all</Link>}>
            <ul className="space-y-2">
              {TEAM_ORDER.map((s) => (
                <li key={s} className="flex items-center justify-between">
                  <StatusChip status={s} />
                  <span className="tabular font-medium text-ink">{d.teamsByStatus[s]}</span>
                </li>
              ))}
            </ul>
          </Card>
          <Card title="Stock" actions={<Link to={paths.district.relief} className="text-sm text-signal hover:underline">Relief supplies</Link>}>
            <p className="text-sm text-muted">
              <span className="tabular font-medium text-ink">{d.stockLines}</span> stock lines held in this district.
            </p>
          </Card>
        </div>
      </div>
    </div>
  )
}
