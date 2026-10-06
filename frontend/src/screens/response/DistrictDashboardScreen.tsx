import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Activity,
  AlertTriangle,
  Boxes,
  Building2,
  Clock,
  Package,
  Truck,
  Users,
} from 'lucide-react'
import { StatusChip } from '@/components/domain'
import { Button, Card, ErrorState, Loading, PageHeader, Stat } from '@/components/ui'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAllocations, useAssignments, useDashboard, useShelters, useStocks, useTeams } from '@/hooks/response/useResponse'
import type { Allocation, Assignment, TeamStatus } from '@/types'
import { formatNumber, formatPercent, relativeTime } from '@/utils/format'

const TEAM_ORDER: TeamStatus[] = ['AVAILABLE', 'DISPATCHED', 'EN_ROUTE', 'ACTIVE', 'OFFLINE_UNKNOWN']

type ActivityFilter = 'all' | 'dispatch' | 'shelter' | 'logistics'

function activityCategory(type: string): ActivityFilter {
  if (type === 'DISPATCH' || type === 'TEAM_STATUS') return 'dispatch'
  if (type === 'SHELTER') return 'shelter'
  if (type === 'RELIEF') return 'logistics'
  return 'all'
}

export function DistrictDashboardScreen() {
  useDocumentTitle('Dashboard')
  const user = useCurrentUser()
  const { districtName, eventName } = useReferenceData()
  const dashboard = useDashboard(user.districtId)
  const teams = useTeams(user.districtId)
  const shelters = useShelters({ districtId: user.districtId })
  const stocks = useStocks(user.districtId)
  const assignments = useAssignments(user.districtId)
  const allocations = useAllocations({ districtId: user.districtId })
  const [activityFilter, setActivityFilter] = useState<ActivityFilter>('all')

  const assignmentList: Assignment[] = Array.isArray(assignments.data)
    ? (assignments.data as Assignment[])
    : Array.isArray((assignments.data as any)?.content)
    ? ((assignments.data as any).content as Assignment[])
    : []

  const openAssignments = assignmentList.filter(
    (a) =>
      a.status === 'UNASSIGNED' ||
      a.status === 'PENDING_ACK' ||
      a.status === 'ACCEPTED' ||
      a.status === 'EN_ROUTE' ||
      a.status === 'ACTIVE'
  )
  const urgentAssignments = openAssignments.filter((a) => a.priority === 1)

  // Real-time disaster updates & actions stream synthesized from all live district channels
  const liveActivities = useMemo(() => {
    const list: Array<{
      id: string
      type: 'WARNING' | 'DISPATCH' | 'TEAM_STATUS' | 'SHELTER' | 'RELIEF'
      message: string
      occurredAt: string
      tone: 'danger' | 'caution' | 'signal' | 'success'
    }> = []

    // 1. Any existing backend activity records
    if (dashboard.data?.activity && Array.isArray(dashboard.data.activity)) {
      dashboard.data.activity.forEach((a) => {
        list.push({
          id: a.id,
          type: a.type as any,
          message: a.message,
          occurredAt: a.occurredAt,
          tone: a.type === 'DISPATCH' ? 'signal' : a.type === 'RELIEF' ? 'caution' : 'signal',
        })
      })
    }

    // 2. Real assignments & dispatch updates
    assignmentList.forEach((a) => {
      if (a.status === 'COMPLETED' && a.assignedAt) {
        list.push({
          id: `comp-${a.id}`,
          type: 'DISPATCH',
          message: `Mission completed: "${a.task}" (${a.locationText})`,
          occurredAt: (a as any).completedAt || a.assignedAt,
          tone: 'success',
        })
      } else if (a.teamId && a.assignedAt) {
        list.push({
          id: `disp-${a.id}`,
          type: 'DISPATCH',
          message: `Team dispatched for "${a.task}" at ${a.locationText}`,
          occurredAt: a.assignedAt,
          tone: a.priority === 1 ? 'danger' : 'signal',
        })
      } else if (a.createdAt) {
        list.push({
          id: `ass-${a.id}`,
          type: 'DISPATCH',
          message: `Rescue requirement registered: "${a.task}" (${a.locationText})`,
          occurredAt: a.createdAt,
          tone: a.priority === 1 ? 'danger' : 'caution',
        })
      }
    })

    // 3. Real rescue team telemetry updates
    if (Array.isArray(teams.data)) {
      teams.data.forEach((t) => {
        if (t.lastStatusAt) {
          const statusText =
            t.status === 'AVAILABLE'
              ? 'is on standby & Available'
              : t.status === 'DISPATCHED'
              ? 'dispatched to incident sector'
              : t.status === 'EN_ROUTE'
              ? 'is en route to target site'
              : t.status === 'ACTIVE'
              ? 'is actively operating on site'
              : 'status updated to offline'
          list.push({
            id: `team-${t.id}-${t.status}`,
            type: 'TEAM_STATUS',
            message: `${t.name} (${t.capacity} personnel) ${statusText}`,
            occurredAt: t.lastStatusAt,
            tone: t.status === 'AVAILABLE' ? 'success' : t.status === 'DISPATCHED' ? 'caution' : 'signal',
          })
        }
      })
    }

    // 4. Real relief supply allocations
    const allocationsList: Allocation[] = Array.isArray(allocations.data)
      ? (allocations.data as Allocation[])
      : Array.isArray((allocations.data as any)?.content)
      ? ((allocations.data as any).content as Allocation[])
      : []

    allocationsList.forEach((al) => {
      if (al.allocatedAt) {
        const sName = shelters.data?.find((s) => s.id === al.shelterId)?.name || 'local shelter'
        const iName = al.itemName || stocks.data?.find((s) => s.id === al.stockId)?.itemName || 'supplies'
        list.push({
          id: `alloc-${al.id}`,
          type: 'RELIEF',
          message: `Relief supply dispatch: ${formatNumber(al.quantity)} ${al.unit || 'units'} of ${iName} allocated to ${sName}`,
          occurredAt: al.allocatedAt,
          tone: 'signal',
        })
      }
    })

    // 5. Real shelter occupancy telemetry
    if (Array.isArray(shelters.data)) {
      shelters.data.forEach((s) => {
        if (s.currentOccupancy > 0) {
          const occPct = Math.round((s.currentOccupancy / s.capacity) * 100)
          list.push({
            id: `shelter-${s.id}`,
            type: 'SHELTER',
            message: `${s.name} shelter operating at ${occPct}% capacity (${s.currentOccupancy}/${s.capacity} persons accommodated)`,
            occurredAt: (s as any).updatedAt || (s as any).createdAt || new Date(Date.now() - 3600000).toISOString(),
            tone: occPct >= 85 ? 'caution' : 'signal',
          })
        }
      })
    }

    // Deduplicate items with same message
    const seen = new Set<string>()
    const unique = list.filter((item) => {
      const key = `${item.type}:${item.message}`
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })

    unique.sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())
    return unique
  }, [dashboard.data, assignmentList, teams.data, allocations.data, shelters.data, stocks.data])

  const filteredActivity = useMemo(() => {
    if (activityFilter === 'all') return liveActivities
    return liveActivities.filter((e) => activityCategory(e.type) === activityFilter)
  }, [liveActivities, activityFilter])

  const sheltersList = useMemo(() => {
    return Array.isArray(shelters.data) ? shelters.data : []
  }, [shelters.data])

  const { totalOccupancy, totalCapacity, occupancyRatio, openSheltersCount, nearlyFullSheltersCount } = useMemo(() => {
    if (sheltersList.length > 0) {
      let occ = 0
      let cap = 0
      let openCount = 0
      let nearlyFull = 0
      sheltersList.forEach((s) => {
        const c = Number(s.capacity) || 0
        const o = Number(s.currentOccupancy) || 0
        occ += o
        cap += c
        if (s.status === 'OPEN') openCount++
        if (c > 0 && o / c >= 0.85) nearlyFull++
      })
      const r = cap > 0 ? occ / cap : 0
      return {
        totalOccupancy: occ,
        totalCapacity: cap,
        occupancyRatio: r,
        openSheltersCount: openCount,
        nearlyFullSheltersCount: nearlyFull,
      }
    }

    // Fallback to dashboard summary values if shelters list not loaded
    const cap = Number(dashboard.data?.shelterCapacity) || 0
    const occ = Number(dashboard.data?.sheltersOccupied) || 0
    const r = cap > 0 ? occ / cap : 0
    return {
      totalOccupancy: occ,
      totalCapacity: cap,
      occupancyRatio: r,
      openSheltersCount: occ > 0 ? 1 : 0,
      nearlyFullSheltersCount: dashboard.data?.nearlyFullShelters || 0,
    }
  }, [sheltersList, dashboard.data])

  if (dashboard.isLoading) return <Loading />
  if (dashboard.isError || !dashboard.data) return <ErrorState error={dashboard.error} onRetry={() => void dashboard.refetch()} />

  const d = dashboard.data

  const rawDistrict = districtName(user.districtId)
  const resolvedDistrict = rawDistrict !== '—' ? rawDistrict : districtName(d.districtId)
  const titleText = resolvedDistrict !== '—' ? `${resolvedDistrict} District` : 'District Dashboard'

  return (
    <div>

      <PageHeader
        title={titleText}
        subtitle={d.activeEventId ? `Active event: ${eventName(d.activeEventId)}` : 'No active disaster event in this district.'}
        actions={
          <Link to={paths.district.newAssignment}>
            <Button>New assignment</Button>
          </Link>
        }
      />

      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Teams available" value={d.teamsByStatus.AVAILABLE} tone="signal" />
        <Stat
          label="Open assignments"
          value={d.openAssignments}
          hint={d.pendingAcknowledgement ? `${d.pendingAcknowledgement} waiting for an answer` : undefined}
          tone={d.pendingAcknowledgement ? 'caution' : 'default'}
        />
        <Stat
          label="Shelter occupancy"
          value={formatPercent(occupancyRatio)}
          hint={
            totalCapacity > 0
              ? `${formatNumber(totalOccupancy)} of ${formatNumber(totalCapacity)} places (${openSheltersCount} open)`
              : 'No shelter capacity'
          }
          tone={nearlyFullSheltersCount > 0 || occupancyRatio >= 0.85 ? 'caution' : 'default'}
        />
        <Stat label="Active warnings" value={d.activeWarnings} tone={d.activeWarnings ? 'danger' : 'default'} />
      </div>

      <div className="grid gap-4 lg:grid-cols-[1fr_360px] items-stretch">
        {/* LEFT COLUMN: Activity & Urgent Tasks */}
        <div className="flex flex-col gap-4">
          {/* Urgent assignments - Deep Crimson / Wine Theme */}
          {urgentAssignments.length > 0 && (
            <Card
              title={
                <div className="flex items-center justify-between w-full">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="h-5 w-5 text-rose-400" />
                    <span className="text-base font-bold text-white">Urgent assignments</span>
                  </div>
                  <span className="rounded-full border border-rose-500/30 bg-rose-500/15 px-2.5 py-0.5 text-xs font-semibold text-rose-300">
                    {urgentAssignments.length} Critical
                  </span>
                </div>
              }
              className="border border-rose-500/35 bg-[#211218] shadow-lg shrink-0"
            >
              <div className="space-y-2.5">
                {urgentAssignments.slice(0, 5).map((a) => (
                  <div
                    key={a.id}
                    className="flex items-start justify-between gap-3 rounded-lg border border-rose-500/25 bg-[#2c161f] p-3 transition-colors hover:border-rose-500/40"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold text-white">{a.task}</p>
                      <p className="mt-0.5 text-xs text-rose-200/70">{a.locationText}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <StatusChip status={a.status} />
                      <span className="rounded bg-rose-500/20 px-1.5 py-0.5 text-[11px] font-mono font-bold text-rose-300">
                        P1
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Recent disaster updates & actions - Deep Midnight Navy Theme */}
          <Card
            title={
              <div className="flex items-center gap-2">
                <Activity className="h-5 w-5 text-cyan-400" />
                <span className="text-base font-bold text-white">Recent disaster updates & actions</span>
              </div>
            }
            className="border border-[#223554] bg-[#0c1526] shadow-lg flex-1 flex flex-col"
            contentClassName="flex-1 flex flex-col min-h-0"
          >
            <div className="flex items-center gap-2 overflow-x-auto pb-2 custom-scrollbar shrink-0">
              {(['all', 'dispatch', 'shelter', 'logistics'] as ActivityFilter[]).map((filter) => (
                <button
                  key={filter}
                  onClick={() => setActivityFilter(filter)}
                  className={`whitespace-nowrap rounded-full border px-3 py-1 text-xs font-semibold transition-all ${
                    activityFilter === filter
                      ? 'border-cyan-400 bg-cyan-950/80 text-cyan-300 shadow-sm'
                      : 'border-[#1e2f4a] bg-[#121e33] text-slate-400 hover:border-slate-600 hover:text-slate-200'
                  }`}
                >
                  {filter === 'all'
                    ? 'All Updates'
                    : filter === 'dispatch'
                    ? 'Dispatches'
                    : filter === 'shelter'
                    ? 'Shelters'
                    : 'Logistics'}
                </button>
              ))}
            </div>

            <div className="mt-3 flex-1 flex flex-col min-h-0">
              {filteredActivity.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center rounded-lg border border-[#1b2a42] bg-[#101b2d] py-8 text-center text-sm text-slate-400">
                  <Activity className="mx-auto mb-2 h-6 w-6 text-slate-500 opacity-60" />
                  <p>No recent actions recorded for this filter category.</p>
                </div>
              ) : (
                <ol className="space-y-2.5 flex-1 min-h-[420px] max-h-[780px] overflow-y-auto pr-2 custom-scrollbar">
                  {filteredActivity.map((e) => (
                    <li
                      key={e.id}
                      className="flex items-start gap-3 rounded-lg border border-[#1d2d46] bg-[#111c2e] p-3 transition-colors hover:border-cyan-500/40"
                    >
                      <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-md border border-[#263a58] bg-[#16243a]">
                        {e.type === 'DISPATCH' && <Truck className="h-4 w-4 text-cyan-400" />}
                        {e.type === 'TEAM_STATUS' && <Users className="h-4 w-4 text-emerald-400" />}
                        {e.type === 'SHELTER' && <Building2 className="h-4 w-4 text-sky-400" />}
                        {e.type === 'RELIEF' && <Package className="h-4 w-4 text-amber-400" />}
                        {e.type === 'WARNING' && <AlertTriangle className="h-4 w-4 text-rose-400" />}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium leading-snug text-slate-100">{e.message}</p>
                        <div className="mt-1 flex items-center gap-1.5 font-mono text-[11px] text-slate-400">
                          <Clock className="h-3 w-3 text-slate-500" />
                          <span>{relativeTime(e.occurredAt)}</span>
                        </div>
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </div>
          </Card>
        </div>

        {/* RIGHT COLUMN: Fleet, Stocks & Shelters */}
        <div className="space-y-4">
          {/* Rescue Teams - Deep Tactical Forest / Emerald Theme */}
          <Card
            title={
              <div className="flex items-center gap-2">
                <Users className="h-5 w-5 text-emerald-400" />
                <span className="text-base font-bold text-white">Rescue teams</span>
              </div>
            }
            actions={
              <Link to={paths.district.teams} className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 hover:underline">
                View all teams →
              </Link>
            }
            className="border border-emerald-500/30 bg-[#0c2020] shadow-lg"
          >
            {teams.isLoading && <Loading />}
            {teams.isError && <ErrorState error={teams.error} onRetry={() => void teams.refetch()} />}
            {teams.data && (
              <ul className="space-y-2">
                {TEAM_ORDER.map((s) => (
                  <li key={s} className="flex items-center justify-between rounded-lg border border-emerald-500/15 bg-[#102929] px-3 py-2">
                    <StatusChip status={s} />
                    <span className="tabular font-mono text-sm font-bold text-emerald-200">{d.teamsByStatus[s]}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          {/* Stocks - Deep Royal Indigo / Violet Warehouse Theme */}
          <Card
            title={
              <div className="flex items-center gap-2">
                <Boxes className="h-5 w-5 text-indigo-400" />
                <span className="text-base font-bold text-white">Stock</span>
              </div>
            }
            actions={
              <Link to={paths.district.relief} className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 hover:underline">
                Relief supplies →
              </Link>
            }
            className="border border-indigo-500/30 bg-[#18142a] shadow-lg"
          >
            <p className="text-xs text-indigo-200/70">
              <span className="tabular font-bold text-white">{d.stockLines}</span> inventory lines managed in this district.
            </p>
            {stocks.isLoading && <Loading />}
            {stocks.isError && <ErrorState error={stocks.error} onRetry={() => void stocks.refetch()} />}
            {stocks.data && stocks.data.length > 0 && (
              <div className="mt-3 space-y-2">
                {stocks.data.slice(0, 5).map((s) => (
                  <div key={s.id} className="flex items-center justify-between rounded-lg border border-indigo-500/20 bg-[#211a3b] px-3 py-2">
                    <span className="text-xs font-medium text-slate-100">{s.itemName}</span>
                    <span className="tabular font-mono text-xs font-semibold text-indigo-200">
                      {formatNumber(s.quantityAvailable)} {s.unit}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </Card>

          {/* Shelters - Deep Oceanic Cyan / Slate Theme */}
          <Card
            title={
              <div className="flex items-center gap-2">
                <Building2 className="h-5 w-5 text-cyan-400" />
                <span className="text-base font-bold text-white">Shelters</span>
              </div>
            }
            actions={
              <Link to={paths.district.shelters} className="text-xs font-semibold text-cyan-400 hover:text-cyan-300 hover:underline">
                View all shelters →
              </Link>
            }
            className="border border-cyan-500/25 bg-[#0d1c29] shadow-lg"
          >
            {shelters.isLoading && <Loading />}
            {shelters.isError && <ErrorState error={shelters.error} onRetry={() => void shelters.refetch()} />}
            {shelters.data && shelters.data.length > 0 && (
              <div className="space-y-2.5">
                {shelters.data.slice(0, 4).map((s) => {
                  const sRatio = s.capacity > 0 ? s.currentOccupancy / s.capacity : s.occupancyRatio || 0
                  return (
                    <div key={s.id} className="space-y-1.5 rounded-lg border border-cyan-500/20 bg-[#122435] p-2.5">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-semibold text-white">{s.name}</span>
                        <StatusChip status={s.status} />
                      </div>
                      <div className="flex items-center justify-between text-[11px] text-slate-300">
                        <span>Occupancy: {s.currentOccupancy} / {s.capacity}</span>
                        <span className="font-mono font-medium text-cyan-300">{formatPercent(sRatio)}</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
            {shelters.data && shelters.data.length > 4 && (
              <p className="mt-2 text-center text-xs text-slate-400">
                Showing 4 of {shelters.data.length} district shelters
              </p>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}
