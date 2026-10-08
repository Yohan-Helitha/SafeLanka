import { Check, ClipboardList, Clock, MapPin, Search, Users, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiErrorNotice } from '@/components/domain'
import { AssignDialog } from '@/components/response/AssignDialog'
import { OfflineToggle } from '@/components/layout/OfflineToggle'
import { Button, ErrorState, Loading } from '@/components/ui'
import { TEAM_TYPE_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAssignments, useCancelAssignment, useShelters, useTeams } from '@/hooks/response/useResponse'
import type { Assignment, AssignmentStatus, RescueTeam, TeamStatus } from '@/types'
import { relativeTime } from '@/utils/format'

type TeamStatusFilter = TeamStatus | 'ALL'
type AssignmentStatusFilter = AssignmentStatus | 'ALL'
type PriorityFilter = 1 | 2 | 3 | 'ALL'

const TEAM_STATUS_OPTIONS: { value: TeamStatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'AVAILABLE', label: 'Available' },
  { value: 'DISPATCHED', label: 'Dispatched' },
  { value: 'EN_ROUTE', label: 'En route' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'OFFLINE_UNKNOWN', label: 'Offline / Unknown' },
]

const ASSIGNMENT_STATUS_OPTIONS: { value: AssignmentStatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'UNASSIGNED', label: 'Unassigned' },
  { value: 'PENDING_ACK', label: 'Pending ack' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'EN_ROUTE', label: 'En route' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const PRIORITY_OPTIONS: { value: PriorityFilter; label: string }[] = [
  { value: 'ALL', label: 'All priorities' },
  { value: 1, label: 'Urgent' },
  { value: 2, label: 'High' },
  { value: 3, label: 'Routine' },
]

function renderTeamStatusBadge(status: TeamStatus) {
  switch (status) {
    case 'AVAILABLE':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-950/60 text-emerald-400 border border-emerald-500/30 shrink-0">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
          Available
        </span>
      )
    case 'DISPATCHED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-amber-950/60 text-amber-300 border border-amber-500/30 shrink-0">
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
          Dispatched
        </span>
      )
    case 'EN_ROUTE':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-cyan-950/60 text-cyan-300 border border-cyan-500/30 shrink-0">
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse" />
          En route
        </span>
      )
    case 'ACTIVE':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-950/60 text-blue-300 border border-blue-500/30 shrink-0">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse" />
          Active
        </span>
      )
    case 'OFFLINE_UNKNOWN':
    default:
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-slate-900 text-slate-400 border border-slate-700 shrink-0">
          <span className="w-1.5 h-1.5 rounded-full bg-slate-500" />
          Offline
        </span>
      )
  }
}

function teamDotClass(status: TeamStatus) {
  switch (status) {
    case 'AVAILABLE':
      return 'bg-emerald-400 animate-pulse'
    case 'DISPATCHED':
      return 'bg-amber-400'
    case 'EN_ROUTE':
      return 'bg-cyan-400 animate-pulse'
    case 'ACTIVE':
      return 'bg-blue-400 animate-pulse'
    case 'OFFLINE_UNKNOWN':
    default:
      return 'bg-slate-500'
  }
}

function renderPriorityBadge(priority: 1 | 2 | 3) {
  switch (priority) {
    case 1:
      return (
        <span className="px-2 py-0.5 rounded text-[11px] font-extrabold uppercase tracking-wider bg-rose-500/10 text-rose-500 border border-rose-500/30 animate-pulse">
          URGENT
        </span>
      )
    case 2:
      return (
        <span className="px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider bg-amber-400/10 text-amber-400 border border-amber-400/30">
          High
        </span>
      )
    case 3:
    default:
      return (
        <span className="px-2 py-0.5 rounded text-[11px] font-medium uppercase tracking-wider bg-slate-500/10 text-slate-400 border border-slate-500/30">
          Routine
        </span>
      )
  }
}

function renderAssignmentStatusBadge(status: AssignmentStatus) {
  switch (status) {
    case 'UNASSIGNED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#1e2638] text-slate-300 border border-slate-700">
          <span className="w-1.5 h-1.5 rounded-full bg-slate-400" />
          Unassigned
        </span>
      )
    case 'PENDING_ACK':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-950/60 text-amber-300 border border-amber-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
          Pending ack
        </span>
      )
    case 'ACCEPTED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-cyan-950/60 text-cyan-300 border border-cyan-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
          Accepted
        </span>
      )
    case 'EN_ROUTE':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-cyan-950/60 text-cyan-300 border border-cyan-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse" />
          En route
        </span>
      )
    case 'ACTIVE':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-950/60 text-blue-300 border border-blue-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse" />
          Active
        </span>
      )
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-950/60 text-emerald-400 border border-emerald-500/30">
          <Check className="w-3 h-3 text-emerald-400 stroke-[3]" />
          Completed
        </span>
      )
    case 'CANCELLED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-900 text-slate-400 border border-slate-800">
          Cancelled
        </span>
      )
    default:
      return null
  }
}

function assignmentStripeColor(priority: 1 | 2 | 3, status: AssignmentStatus) {
  if (status === 'COMPLETED') return 'bg-emerald-500'
  if (status === 'CANCELLED') return 'bg-slate-600'
  if (priority === 1) return 'bg-rose-500'
  if (priority === 2) return 'bg-amber-400'
  return 'bg-cyan-500'
}

function FilterBar({
  searchPlaceholder,
  searchValue,
  onSearchChange,
  filters,
  onClearSearch,
}: {
  searchPlaceholder: string
  searchValue: string
  onSearchChange: (value: string) => void
  filters: React.ReactNode
  onClearSearch?: () => void
}) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" aria-hidden />
        <input
          type="text"
          value={searchValue}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={searchPlaceholder}
          className="w-full rounded-lg border border-[#232c3f] bg-[#0c121e] py-2 pl-9 pr-9 text-sm text-slate-200 placeholder:text-slate-500 focus:border-cyan-500 focus:outline-none"
        />
        {searchValue && (
          <button
            type="button"
            onClick={onClearSearch}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
          >
            <X className="h-3.5 w-3.5" aria-hidden />
          </button>
        )}
      </div>
      <div className="flex items-center gap-2">{filters}</div>
    </div>
  )
}

export function RescueTeamsScreen() {
  useDocumentTitle('Rescue teams')
  const user = useCurrentUser()
  const { districtName } = useReferenceData()
  const navigate = useNavigate()
  const { toast } = useToast()
  const teams = useTeams(user.districtId)
  const allTeamsQuery = useTeams()
  const assignments = useAssignments(user.districtId)
  const shelters = useShelters({ districtId: user.districtId })
  const cancel = useCancelAssignment()
  const [assigning, setAssigning] = useState<Assignment | null>(null)

  const teamsList: RescueTeam[] = useMemo(
    () => (Array.isArray(teams.data) ? (teams.data as RescueTeam[]) : []),
    [teams.data],
  )
  const assignmentsList: Assignment[] = useMemo(
    () =>
      Array.isArray(assignments.data)
        ? (assignments.data as Assignment[])
        : Array.isArray((assignments.data as any)?.content)
        ? ((assignments.data as any).content as Assignment[])
        : [],
    [assignments.data],
  )

  const teamMap = useMemo(() => {
    const map = new Map<string, RescueTeam>()
    if (Array.isArray(allTeamsQuery.data)) {
      allTeamsQuery.data.forEach((t) => map.set(t.id, t))
    }
    teamsList.forEach((t) => map.set(t.id, t))
    return map
  }, [teamsList, allTeamsQuery.data])

  const shelterMap = useMemo(() => {
    const map = new Map<string, string>()
    if (Array.isArray(shelters.data)) {
      shelters.data.forEach((s) => map.set(s.id, s.name))
    }
    return map
  }, [shelters.data])

  const [teamSearch, setTeamSearch] = useState('')
  const [teamStatusFilter, setTeamStatusFilter] = useState<TeamStatusFilter>('ALL')
  const [assignmentSearch, setAssignmentSearch] = useState('')
  const [assignmentStatusFilter, setAssignmentStatusFilter] = useState<AssignmentStatusFilter>('ALL')
  const [priorityFilter, setPriorityFilter] = useState<PriorityFilter>('ALL')

  const filteredTeams = teamsList.filter((t) => {
    const matchesSearch = t.name.toLowerCase().includes(teamSearch.toLowerCase())
    const matchesStatus = teamStatusFilter === 'ALL' || t.status === teamStatusFilter
    return matchesSearch && matchesStatus
  })

  const enrichedAssignments = useMemo<Assignment[]>(() => {
    return assignmentsList.map((a) => {
      const matchedTeam = a.teamId ? teamMap.get(a.teamId) : undefined
      const resolvedShelterName =
        a.destinationShelterName || (a.destinationShelterId ? shelterMap.get(a.destinationShelterId) ?? null : null)
      return {
        ...a,
        teamName: a.teamName || matchedTeam?.name || null,
        teamType: (a as any).teamType || matchedTeam?.teamType,
        destinationShelterName: resolvedShelterName ?? null,
      } as Assignment
    })
  }, [assignmentsList, teamMap, shelterMap])

  const filteredAssignments = enrichedAssignments.filter((a) => {
    const query = assignmentSearch.toLowerCase()
    const matchesSearch =
      a.task.toLowerCase().includes(query) ||
      a.locationText.toLowerCase().includes(query) ||
      (a.teamName ?? '').toLowerCase().includes(query)
    const matchesStatus = assignmentStatusFilter === 'ALL' || a.status === assignmentStatusFilter
    const matchesPriority = priorityFilter === 'ALL' || a.priority === priorityFilter
    return matchesSearch && matchesStatus && matchesPriority
  })

  const totalTeams = teamsList.length
  const availableTeams = teamsList.filter((t) => t.status === 'AVAILABLE').length
  const dispatchedTeams = teamsList.filter((t) => t.status === 'DISPATCHED' || t.status === 'EN_ROUTE').length

  const activeTasks = assignmentsList.filter((a) => a.status !== 'COMPLETED' && a.status !== 'CANCELLED').length
  const urgentTasks = assignmentsList.filter(
    (a) => a.priority === 1 && a.status !== 'COMPLETED' && a.status !== 'CANCELLED'
  ).length

  return (
    <div className="space-y-6">
      {/* Header Section with Title, Status telemetry, Quick KPIs & Action Button */}
      <header className="space-y-4">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="font-display text-2xl font-semibold leading-tight text-ink sm:text-[28px]">Rescue teams</h1>
              <span className="rounded-full border border-cyan-500/30 bg-cyan-950/60 px-2.5 py-0.5 font-mono text-xs font-semibold text-cyan-400">
                {districtName(user.districtId)} Sector
              </span>
            </div>
            <p className="mt-1 text-sm text-slate-400">
              See who is available, and send teams where they are needed.
            </p>
          </div>

          <div className="flex items-center gap-3 self-start md:self-auto">
            <OfflineToggle />
            <Button size="md" onClick={() => navigate(paths.district.newAssignment)}>
              New assignment
            </Button>
          </div>
        </div>

        {/* Tactical KPI Summary Ribbon */}
        <div className="grid grid-cols-2 gap-3 pt-1 sm:grid-cols-4">
          <div className="flex items-center justify-between rounded-xl border border-[#232c3f] bg-[#161b29]/90 p-3">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Total Teams</p>
              <p className="mt-0.5 text-xl font-bold text-white">{totalTeams}</p>
            </div>
            <span className="h-2.5 w-2.5 rounded-full bg-blue-400 animate-pulse" />
          </div>

          <div className="flex items-center justify-between rounded-xl border border-[#232c3f] bg-[#161b29]/90 p-3">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Available / Ready</p>
              <p className="mt-0.5 text-xl font-bold text-emerald-400">{availableTeams}</p>
            </div>
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-400 animate-pulse" />
          </div>

          <div className="flex items-center justify-between rounded-xl border border-[#232c3f] bg-[#161b29]/90 p-3">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Dispatched</p>
              <p className="mt-0.5 text-xl font-bold text-amber-400">{dispatchedTeams}</p>
            </div>
            <span className="h-2.5 w-2.5 rounded-full bg-amber-400" />
          </div>

          <div className="flex items-center justify-between rounded-xl border border-[#232c3f] bg-[#161b29]/90 p-3">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Active Tasks</p>
              <p className="mt-0.5 text-xl font-bold text-white">{activeTasks}</p>
            </div>
            {urgentTasks > 0 ? (
              <span className="rounded border border-rose-500/30 bg-rose-500/10 px-2 py-0.5 font-mono text-xs font-semibold text-rose-400">
                {urgentTasks} urgent
              </span>
            ) : (
              <span className="rounded border border-slate-700 bg-slate-800/60 px-2 py-0.5 font-mono text-xs font-semibold text-slate-400">
                0 urgent
              </span>
            )}
          </div>
        </div>
      </header>

      {/* Main Dispatch Command Grid: Left (Teams Fleet) & Right (Assignments Queue) */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* LEFT: Teams Readiness Fleet (5 Columns) */}
        <section
          className="space-y-4 rounded-2xl border border-cyan-900/30 bg-[#0c121e]/95 p-4 shadow-sm sm:p-5 lg:col-span-5"
          data-purpose="teams-section"
        >
          <div className="flex items-center justify-between border-b border-cyan-900/30 pb-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 shadow-sm">
                <Users className="h-4.5 w-4.5" />
              </div>
              <div>
                <h3 className="text-base font-bold tracking-tight text-white">Teams</h3>
                <p className="text-xs text-slate-400">Readiness & fleet status</p>
              </div>
            </div>
            <span className="rounded-full border border-cyan-500/30 bg-cyan-950/60 px-2.5 py-1 font-mono text-xs font-semibold text-cyan-400">
              {filteredTeams.length} of {teamsList.length}
            </span>
          </div>

          <FilterBar
            searchPlaceholder="Search teams..."
            searchValue={teamSearch}
            onSearchChange={setTeamSearch}
            onClearSearch={() => setTeamSearch('')}
            filters={
              <select
                value={teamStatusFilter}
                onChange={(e) => setTeamStatusFilter(e.target.value as TeamStatusFilter)}
                className="min-h-[36px] rounded-lg border border-[#232c3f] bg-[#0c121e] px-3 text-xs text-slate-200 focus:border-cyan-500 focus:outline-none"
              >
                {TEAM_STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            }
          />

          {teams.isLoading && <Loading />}
          {teams.isError && <ErrorState error={teams.error} onRetry={() => void teams.refetch()} />}

          {filteredTeams.length === 0 && !teams.isLoading && !teams.isError && (
            <p className="py-8 text-center text-sm text-slate-400">No teams match the current filters.</p>
          )}

          <div className="space-y-3" id="teams-tactical-list">
            {filteredTeams.map((t: RescueTeam) => {
              const isDispatched = t.status === 'DISPATCHED'
              return (
                <div
                  key={t.id}
                  className={`space-y-3 rounded-xl border bg-[#131b2c] p-4 shadow-sm transition-all hover:bg-[#172238] ${
                    isDispatched
                      ? 'border-amber-500/30 hover:border-amber-500/50'
                      : 'border-[#1e2a42] hover:border-cyan-500/40'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`h-2 w-2 rounded-full ${teamDotClass(t.status)}`} />
                        <h4 className="text-sm font-semibold leading-snug tracking-tight text-white">{t.name}</h4>
                      </div>
                      <p className="mt-1 flex items-center gap-1.5 text-xs text-slate-400">
                        <span className="rounded border border-[#232c3f] bg-[#0c121e] px-2 py-0.5 text-[11px] font-medium text-slate-300">
                          {TEAM_TYPE_LABEL[t.teamType] ?? t.teamType}
                        </span>
                        <span>·</span>
                        <span className="font-mono text-xs font-medium text-slate-300">{t.capacity} people</span>
                      </p>
                    </div>
                    {renderTeamStatusBadge(t.status)}
                  </div>

                  <div className="flex items-center gap-1.5 border-t border-[#1e2a42]/70 pt-2 text-xs text-slate-400">
                    <Clock className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                    <span>Last update:</span>
                    <span className="font-medium text-slate-300">{relativeTime(t.lastStatusAt)}</span>
                  </div>
                </div>
              )
            })}
          </div>
        </section>

        {/* RIGHT: Incident Assignments & Dispatch Queue (7 Columns) */}
        <section
          className="space-y-4 rounded-2xl border border-[#253046] bg-[#161e2e]/95 p-4 shadow-sm sm:p-5 lg:col-span-7"
          data-purpose="assignments-section"
        >
          <div className="flex items-center justify-between border-b border-[#253046] pb-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-400 shadow-sm">
                <ClipboardList className="h-4.5 w-4.5" />
              </div>
              <div>
                <h3 className="text-base font-bold tracking-tight text-white">Assignments</h3>
                <p className="text-xs text-slate-400">Realtime dispatch queue</p>
              </div>
            </div>
            <span className="rounded-full border border-[#253046] bg-[#1e273a] px-2.5 py-1 font-mono text-xs font-semibold text-slate-300">
              {filteredAssignments.length} of {assignmentsList.length}
            </span>
          </div>

          <FilterBar
            searchPlaceholder="Search assignments..."
            searchValue={assignmentSearch}
            onSearchChange={setAssignmentSearch}
            onClearSearch={() => setAssignmentSearch('')}
            filters={
              <>
                <select
                  value={assignmentStatusFilter}
                  onChange={(e) => setAssignmentStatusFilter(e.target.value as AssignmentStatusFilter)}
                  className="min-h-[36px] rounded-lg border border-[#232c3f] bg-[#0c121e] px-3 text-xs text-slate-200 focus:border-amber-500 focus:outline-none"
                >
                  {ASSIGNMENT_STATUS_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <select
                  value={priorityFilter}
                  onChange={(e) => setPriorityFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value) as PriorityFilter)}
                  className="min-h-[36px] rounded-lg border border-[#232c3f] bg-[#0c121e] px-3 text-xs text-slate-200 focus:border-amber-500 focus:outline-none"
                >
                  {PRIORITY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </>
            }
          />

          <ApiErrorNotice error={cancel.error} />
          {assignments.isLoading && <Loading />}
          {assignments.isError && <ErrorState error={assignments.error} onRetry={() => void assignments.refetch()} />}

          {filteredAssignments.length === 0 && !assignments.isLoading && !assignments.isError && (
            <p className="py-8 text-center text-sm text-slate-400">No assignments match the current filters.</p>
          )}

          <div className="space-y-3" id="assignments-tactical-list">
            {filteredAssignments.map((a: Assignment) => {
              const borderClass =
                a.priority === 1
                  ? 'border-rose-500/40 hover:border-rose-500/60'
                  : a.status === 'UNASSIGNED'
                    ? 'border-cyan-500/30 hover:border-cyan-500/50'
                    : 'border-[#222d42] hover:border-cyan-500/30'

              const isCompleted = a.status === 'COMPLETED'
              const isCancelled = a.status === 'CANCELLED'

              return (
                <div
                  key={a.id}
                  className={`relative space-y-3.5 overflow-hidden rounded-xl border bg-[#121927] p-4 shadow-sm transition-all hover:bg-[#172033] ${borderClass} ${
                    isCompleted || isCancelled ? 'opacity-85' : ''
                  }`}
                >
                  {/* Left priority / status stripe */}
                  <div className={`absolute bottom-0 left-0 top-0 w-1 ${assignmentStripeColor(a.priority, a.status)}`} />

                  <div className="flex flex-col justify-between gap-3 pl-1 sm:flex-row sm:items-start">
                    <div className="flex-1 space-y-1.5">
                      <div className="flex flex-wrap items-center gap-2">
                        {renderPriorityBadge(a.priority)}
                      </div>
                      <h4
                        className={`text-sm font-semibold leading-snug ${
                          isCompleted ? 'text-slate-200' : 'text-white'
                        }`}
                      >
                        {a.task}
                      </h4>
                      <div className="flex items-center gap-1.5 pt-0.5 text-xs text-slate-400">
                        <MapPin className="w-3.5 h-3.5 shrink-0 text-slate-500" />
                        <span>{a.locationText}</span>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-start">
                      {renderAssignmentStatusBadge(a.status)}
                    </div>
                  </div>

                  {/* Dedicated Assigned Team Section - High Visibility */}
                  {a.teamName ? (
                    <div className="flex items-center justify-between rounded-lg border border-cyan-500/40 bg-gradient-to-r from-cyan-950/60 to-[#121c2c] p-2.5 shadow-sm transition-colors hover:border-cyan-500/60">
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-cyan-500/30 bg-cyan-500/10 text-cyan-400">
                          <Users className="h-4 w-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-[10px] font-semibold uppercase tracking-wider text-cyan-400">
                            {isCompleted ? 'Completed By' : 'Assigned Rescue Team'}
                          </div>
                          <div className="truncate text-sm font-bold text-white">
                            {a.teamName}
                          </div>
                        </div>
                      </div>
                      {(a as any).teamType && (
                        <span className="shrink-0 rounded border border-cyan-500/30 bg-cyan-900/50 px-2 py-0.5 font-mono text-[11px] font-medium text-cyan-200">
                          {TEAM_TYPE_LABEL[(a as any).teamType as keyof typeof TEAM_TYPE_LABEL] ?? (a as any).teamType}
                        </span>
                      )}
                    </div>
                  ) : (
                    <div className="flex items-center justify-between rounded-lg border border-amber-500/25 bg-amber-950/20 px-3 py-2 text-xs">
                      <div className="flex items-center gap-2 text-amber-300">
                        <Users className="h-4 w-4 text-amber-400 shrink-0" />
                        <span className="font-medium text-amber-200">No rescue team assigned</span>
                      </div>
                      <span className="rounded border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-amber-400">
                        Awaiting dispatch
                      </span>
                    </div>
                  )}

                  <div className="flex items-center justify-between border-t border-[#222d42]/80 pl-1 pt-3">
                    <div className="flex items-center gap-3 text-xs text-slate-400">
                      {a.peopleEstimated > 0 && (
                        <span className="inline-flex items-center gap-1 font-medium text-slate-300">
                          <span className="text-amber-400">●</span> {a.peopleEstimated} people trapped
                        </span>
                      )}
                      {a.destinationShelterName && (
                        <span className="text-slate-400">
                          → Shelter: <span className="font-medium text-cyan-300">{a.destinationShelterName}</span>
                        </span>
                      )}
                      {!a.peopleEstimated && !a.destinationShelterName && (
                        <span className="font-mono text-[11px] text-slate-500">
                          ID: {a.id.slice(0, 8)}
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-3">
                      {a.status === 'UNASSIGNED' && (
                        <Button size="md" onClick={() => setAssigning(a)}>
                          Assign team
                        </Button>
                      )}
                      {!isCompleted && !isCancelled && (
                        <Button
                          size="md"
                          variant="ghost"
                          disabled={cancel.isPending && cancel.variables === a.id}
                          onClick={() =>
                            cancel.mutate(a.id, {
                              onSuccess: () => toast('Assignment cancelled'),
                            })
                          }
                        >
                          {cancel.isPending && cancel.variables === a.id ? 'Cancelling...' : 'Cancel'}
                        </Button>
                      )}
                      {(isCompleted || isCancelled) && <span className="select-none text-xs text-slate-600">—</span>}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </section>
      </div>

      {assigning && <AssignDialog assignment={assigning} onClose={() => setAssigning(null)} />}
    </div>
  )
}
