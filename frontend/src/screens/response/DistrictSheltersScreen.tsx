import { useMemo, useState } from 'react'
import { Home, MapPin, Search, Users, X } from 'lucide-react'
import { HeadcountDialog } from '@/components/response/HeadcountDialog'
import { EmptyState, ErrorState, Loading } from '@/components/ui'
import { MapboxMap } from '@/components/ui/MapboxMap'
import type { MapMarker } from '@/components/ui/MapboxMap'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useShelters } from '@/hooks/response/useResponse'
import type { OccupancyLevel, Shelter, ShelterStatus } from '@/types'
import { formatPercent } from '@/utils/format'

type StatusFilter = 'ALL' | 'OPEN' | 'FULL' | 'CLOSED'

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'OPEN', label: 'Open' },
  { value: 'FULL', label: 'Full' },
  { value: 'CLOSED', label: 'Closed' },
]

function statusColor(status: string) {
  switch (status) {
    case 'OPEN':
      return {
        badge: 'bg-emerald-950/80 text-emerald-400 border-emerald-500/40',
        dot: 'bg-emerald-400',
        tone: 'ok' as const,
        label: 'Open',
      }
    case 'FULL':
      return {
        badge: 'bg-rose-950/80 text-rose-300 border-rose-500/40',
        dot: 'bg-rose-500',
        tone: 'danger' as const,
        label: 'Full',
      }
    case 'CLOSED':
      return {
        badge: 'bg-slate-900 text-slate-400 border-slate-700',
        dot: 'bg-slate-500',
        tone: 'signal' as const,
        label: 'Closed',
      }
    default:
      return {
        badge: 'bg-slate-900 text-slate-400 border-slate-700',
        dot: 'bg-slate-500',
        tone: 'signal' as const,
        label: status,
      }
  }
}

function renderLevelBadge(level: string) {
  if (level === 'FULL') {
    return (
      <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-rose-950/80 text-rose-300 border border-rose-500/40">
        Critical Status
      </span>
    )
  }
  if (level === 'AMBER') {
    return (
      <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-amber-500/30 text-amber-300 border border-amber-500/50">
        Nearly Full
      </span>
    )
  }
  return (
    <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-cyan-950/40 text-cyan-400 border border-cyan-500/30">
      Normal Status
    </span>
  )
}

function occupancyBarColor(level: string) {
  if (level === 'FULL') return 'bg-rose-500'
  if (level === 'AMBER') return 'bg-amber-400'
  return 'bg-emerald-400'
}

function shelterCardStyle(level: string, status: string, isSelected: boolean) {
  if (isSelected) {
    return 'bg-[#152338] border-cyan-400 ring-2 ring-cyan-500/40 shadow-cyan-500/20'
  }
  if (status === 'CLOSED') {
    return 'bg-[#10141e] hover:bg-[#141926] border-slate-700/60'
  }
  if (level === 'FULL' || status === 'FULL') {
    return 'bg-[#231218] hover:bg-[#2c161f] border-rose-500/50 hover:border-rose-400/80 shadow-[0_4px_20px_rgba(244,63,94,0.15)]'
  }
  if (level === 'AMBER') {
    return 'bg-[#231a10] hover:bg-[#2c2014] border-amber-500/50 hover:border-amber-400/80 shadow-[0_4px_20px_rgba(245,158,11,0.15)]'
  }
  return 'bg-[#0e2122] hover:bg-[#12292b] border-emerald-500/40 hover:border-emerald-400/80 shadow-[0_4px_20px_rgba(16,185,129,0.15)]'
}

export function DistrictSheltersScreen() {
  useDocumentTitle('Shelters')
  const user = useCurrentUser()
  const { districtName } = useReferenceData()
  const shelters = useShelters({ districtId: user.districtId })
  const [counting, setCounting] = useState<Shelter | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [selectedShelterId, setSelectedShelterId] = useState<string | null>(null)

  const rawData: Shelter[] = Array.isArray(shelters.data)
    ? (shelters.data as Shelter[])
    : Array.isArray((shelters.data as any)?.content)
    ? ((shelters.data as any).content as Shelter[])
    : []

  const data: Shelter[] = useMemo(() => {
    return rawData.map((s) => {
      const cap = Number(s.capacity) || 0
      const occ = Number(s.currentOccupancy) || 0
      const free = s.freeCapacity != null && !Number.isNaN(s.freeCapacity) ? s.freeCapacity : Math.max(0, cap - occ)
      const ratio =
        s.occupancyRatio != null && Number.isFinite(s.occupancyRatio)
          ? s.occupancyRatio
          : cap > 0
          ? occ / cap
          : 0
      const isFull = s.status === 'FULL' || occ >= cap || free === 0
      const isAmber = !isFull && (s.level === 'AMBER' || ratio >= 0.85)
      const level: OccupancyLevel = s.level ?? (isFull ? 'FULL' : isAmber ? 'AMBER' : 'OK')
      const status: ShelterStatus = isFull && s.status !== 'CLOSED' ? 'FULL' : s.status

      return {
        ...s,
        capacity: cap,
        currentOccupancy: occ,
        freeCapacity: free,
        occupancyRatio: ratio,
        level,
        status,
      }
    })
  }, [rawData])

  const filtered = useMemo(() => {
    return data.filter((s) => {
      const q = search.toLowerCase().trim()
      const matchesSearch = !q || s.name.toLowerCase().includes(q) || s.address.toLowerCase().includes(q)
      const matchesStatus = statusFilter === 'ALL' || s.status === statusFilter
      return matchesSearch && matchesStatus
    })
  }, [data, search, statusFilter])

  const totalCapacity = data.reduce((n, s) => n + s.capacity, 0)
  const totalOccupancy = data.reduce((n, s) => n + s.currentOccupancy, 0)
  const occupancyRatio = totalCapacity > 0 ? totalOccupancy / totalCapacity : 0
  const normalCount = data.filter((s) => s.level === 'OK').length
  const amberCount = data.filter((s) => s.level === 'AMBER').length
  const fullCount = data.filter((s) => s.level === 'FULL').length
  const openCount = data.filter((s) => s.status === 'OPEN').length

  const mapMarkers = useMemo<MapMarker[]>(() => {
    return filtered.map((s) => {
      const pct = Math.round(s.occupancyRatio * 100)
      const tone: MapMarker['tone'] = s.level === 'FULL' ? 'danger' : s.level === 'AMBER' ? 'caution' : 'ok'
      const statusText = s.status === 'OPEN' ? 'Open' : s.status === 'FULL' ? 'Full' : 'Closed'
      const colorHex = s.level === 'FULL' ? '#f87171' : s.level === 'AMBER' ? '#fbbf24' : '#34d399'
      const free = s.freeCapacity

      const popupHtml = `
        <div style="background:#0f172a;color:#f8fafc;padding:14px;font-family:Inter,sans-serif;border-radius:12px;border:1px solid #334155;box-shadow:0 20px 25px -5px rgba(0,0,0,0.5);">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:10px;margin-bottom:8px;">
            <div style="font-weight:800;font-size:13px;color:#ffffff;line-height:1.25;letter-spacing:0.2px;">${s.name}</div>
            <span style="font-size:10px;font-weight:800;padding:3px 8px;border-radius:6px;background:${colorHex}22;color:${colorHex};border:1px solid ${colorHex}88;white-space:nowrap;letter-spacing:0.4px;">
              ${pct}% • ${statusText}
            </span>
          </div>
          <div style="font-size:11px;color:#cbd5e1;margin-bottom:10px;font-weight:500;">${s.address}</div>
          <div style="display:flex;align-items:center;justify-content:space-between;font-size:11px;margin-bottom:6px;">
            <span style="color:#f1f5f9;font-weight:700;">${s.currentOccupancy} / ${s.capacity} occupants</span>
            <span style="color:${colorHex};font-weight:700;">${free} free</span>
          </div>
          <div style="width:100%;height:6px;background:#020617;border-radius:9999px;overflow:hidden;border:1px solid #1e293b;">
            <div style="width:${Math.min(100, pct)}%;height:100%;background:${colorHex};border-radius:9999px;box-shadow:0 0 8px ${colorHex}88;"></div>
          </div>
        </div>
      `

      return {
        id: s.id,
        latitude: s.latitude,
        longitude: s.longitude,
        label: `${s.name} (${pct}%)`,
        tone,
        popupHtml,
      }
    })
  }, [filtered])

  return (
    <div className="space-y-6">
      {/* Top Header & Tactical KPI Summary */}
      <header className="rounded-2xl border border-[#222d42] bg-[#0d1320]/80 p-5 shadow-lg backdrop-blur-sm sm:p-6">
        <div className="flex flex-col gap-4 w-full">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <div className="flex items-center space-x-2.5 flex-wrap">
                <h2 className="text-2xl font-bold text-white tracking-tight">Shelters</h2>
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-800 border border-slate-700 text-slate-300">
                  {districtName(user.districtId)} District Command
                </span>
              </div>
              <p className="text-xs lg:text-sm text-slate-400 mt-1">
                Occupancy across the shelters in your district.{' '}
                <span className="text-amber-400 font-medium">Amber starts at 90% full.</span>
              </p>
            </div>
            <div className="flex items-center space-x-2 text-xs font-medium text-slate-400 self-start sm:self-auto">
              <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse shadow-sm shadow-cyan-400/50" />
              <span>Live district telemetry</span>
            </div>
          </div>

          {/* 3 KPI Command Cards */}
          <div className="w-full grid grid-cols-1 md:grid-cols-3 gap-3.5 pt-1">
            {/* Total Shelters */}
            <div className="bg-[#161b29] border border-[#222d42] rounded-xl p-3.5 flex items-center justify-between shadow-sm">
              <div className="flex flex-col">
                <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 mb-1">Total Shelters</span>
                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-extrabold text-white leading-tight">{data.length}</span>
                  {data.length > 0 && (
                    <span className="text-xs font-semibold text-emerald-400 bg-emerald-950/80 border border-emerald-500/40 px-2 py-0.5 rounded-full">
                      {openCount === data.length ? 'All Operational' : `${openCount} Open`}
                    </span>
                  )}
                </div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-[#141c2c] border border-[#222d42] flex items-center justify-center text-cyan-400">
                <Home className="w-5 h-5" />
              </div>
            </div>

            {/* Capacity & Occupancy */}
            <div className="bg-[#161b29] border border-[#222d42] rounded-xl p-3.5 flex flex-col justify-between shadow-sm">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400">Capacity &amp; Occupancy</span>
                <span className="text-xs font-bold text-cyan-400 bg-cyan-950/40 border border-cyan-500/30 px-2 py-0.5 rounded">
                  {formatPercent(occupancyRatio)} District Total
                </span>
              </div>
              <div className="flex items-baseline space-x-2 mb-2">
                <span className="text-2xl font-extrabold text-cyan-400 leading-tight tracking-tight">
                  {totalOccupancy.toLocaleString()}
                </span>
                <span className="text-xs font-medium text-slate-400">/ {totalCapacity.toLocaleString()} occupants</span>
              </div>
              <div className="w-full h-2 rounded-full bg-[#0d1320] overflow-hidden p-0.5 border border-[#222d42]/70">
                <div
                  className="h-full rounded-full bg-cyan-400 transition-all duration-500 shadow-sm"
                  style={{ width: `${Math.min(100, Math.round(occupancyRatio * 100))}%` }}
                />
              </div>
            </div>

            {/* Status Threshold Breakdown */}
            <div className="bg-[#161b29] border border-[#222d42] rounded-xl p-3.5 flex flex-col justify-between shadow-sm">
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 mb-2">
                Status Threshold Breakdown
              </span>
              <div className="grid grid-cols-3 gap-2">
                <div className="bg-[#141c2c] border border-[#222d42] rounded-lg p-2 flex flex-col items-center">
                  <div className="flex items-center space-x-1.5 mb-0.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-400" />
                    <span className="text-base font-bold text-white leading-none">{normalCount}</span>
                  </div>
                  <span className="text-[10px] font-medium text-slate-400">&lt;90% Normal</span>
                </div>
                <div className="bg-[#141c2c] border border-[#222d42] rounded-lg p-2 flex flex-col items-center">
                  <div className="flex items-center space-x-1.5 mb-0.5">
                    <span className="w-2 h-2 rounded-full bg-amber-400" />
                    <span className="text-base font-bold text-amber-300 leading-none">{amberCount}</span>
                  </div>
                  <span className="text-[10px] font-medium text-slate-400">≥90% Warning</span>
                </div>
                <div className="bg-[#141c2c] border border-[#222d42] rounded-lg p-2 flex flex-col items-center">
                  <div className="flex items-center space-x-1.5 mb-0.5">
                    <span className="w-2 h-2 rounded-full bg-rose-500" />
                    <span className="text-base font-bold text-rose-300 leading-none">{fullCount}</span>
                  </div>
                  <span className="text-[10px] font-medium text-slate-400">100% Critical</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Operational Split Workspace (Cards List on Left + Existing Leaflet Map on Right) */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        {/* Left: Shelter Cards Column */}
        <div className="lg:col-span-6 xl:col-span-6 flex flex-col space-y-4">
          {/* Section Toolbar */}
          <div className="flex items-center justify-between pb-1">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <Home className="w-3.5 h-3.5 text-cyan-400" />
              Assigned Shelters ({filtered.length})
            </span>
            <span className="text-[11px] text-slate-500">Real-time telemetry</span>
          </div>

          {/* Search & Filter Toolbar */}
          <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" aria-hidden />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search shelters by name or location..."
                className="w-full rounded-lg border border-[#222d42] bg-[#0d1320] py-2 pl-9 pr-9 text-xs text-slate-200 placeholder:text-slate-500 focus:border-cyan-500 focus:outline-none"
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                >
                  <X className="h-3.5 w-3.5" aria-hidden />
                </button>
              )}
            </div>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
              className="min-h-[34px] rounded-lg border border-[#222d42] bg-[#0d1320] px-3 text-xs text-slate-200 focus:border-cyan-500 focus:outline-none"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          {shelters.isLoading && <Loading />}
          {shelters.isError && <ErrorState error={shelters.error} onRetry={() => void shelters.refetch()} />}
          {filtered.length === 0 && !shelters.isLoading && !shelters.isError && (
            <EmptyState icon={MapPin} title="No shelters match the current filters" />
          )}

          {/* List of Shelter Command Cards (With custom command scrollbar replacing default scrollbar) */}
          <div
            className="space-y-4 max-h-[calc(100vh-280px)] overflow-y-auto pr-1.5 custom-command-scrollbar"
            style={{ scrollbarWidth: 'thin', scrollbarColor: '#222d42 #0d1320' }}
          >
            {filtered.map((s) => {
              const colors = statusColor(s.status)
              const free = s.freeCapacity
              const pct = Math.min(100, Math.round(s.occupancyRatio * 100))
              const isSelected = selectedShelterId === s.id
              const isFull = s.level === 'FULL' || s.status === 'FULL'

              return (
                <article
                  key={s.id}
                  onClick={() => setSelectedShelterId(s.id)}
                  className={`transition-all duration-200 border rounded-xl p-5 shadow-lg relative group overflow-hidden cursor-pointer ${shelterCardStyle(
                    s.level,
                    s.status,
                    isSelected
                  )}`}
                >
                  <div className="flex items-start justify-between gap-3 mb-4">
                    <div className="space-y-1.5">
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <h3 className="text-base font-bold text-white group-hover:text-cyan-300 transition-colors tracking-tight">
                          {s.name}
                        </h3>
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border shadow-sm ${colors.badge}`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${colors.dot} ${s.status === 'OPEN' ? 'animate-ping' : ''}`} />
                          {colors.label}
                        </span>
                        {renderLevelBadge(s.level)}
                      </div>
                      <div className="flex items-center text-xs text-slate-400 space-x-1.5">
                        <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span>
                          {s.address}, {districtName(s.districtId)}
                        </span>
                      </div>
                    </div>

                    {s.status !== 'CLOSED' && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          setCounting(s)
                        }}
                        className="shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-xs font-semibold text-white transition-all focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:ring-offset-2 focus:ring-offset-[#0d1320] active:scale-95 shadow-sm"
                        type="button"
                      >
                        <Users className="w-3.5 h-3.5 text-white" />
                        Update headcount
                      </button>
                    )}
                  </div>

                  <div className="pt-3 border-t border-[#1e273a]">
                    <div className="flex justify-between items-end mb-2">
                      <div>
                        <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block mb-0.5">
                          Shelter Occupancy
                        </span>
                        <div className="flex items-baseline space-x-1.5">
                          <span className="text-white font-extrabold text-xl tracking-tight leading-none">
                            {s.currentOccupancy.toLocaleString()}
                          </span>
                          <span className="text-slate-400 text-xs font-medium">/ {s.capacity.toLocaleString()}</span>
                          <span
                            className={`ml-2 text-xs font-bold px-2 py-0.5 rounded border ${
                              isFull
                                ? 'text-rose-300 bg-rose-950/80 border-rose-500/40'
                                : s.level === 'AMBER'
                                ? 'text-amber-300 bg-amber-500/30 border-amber-500/50'
                                : 'text-emerald-400 bg-emerald-950/80 border-emerald-500/40'
                            }`}
                          >
                            {pct}% {isFull ? 'Full' : 'Filled'}
                          </span>
                        </div>
                      </div>
                      <div className="text-right">
                        <span
                          className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-md border ${
                            isFull
                              ? 'text-rose-300 bg-rose-950/70 border-rose-500/50'
                              : s.level === 'AMBER'
                              ? 'text-amber-300 bg-amber-950/70 border-amber-500/50'
                              : 'text-emerald-300 bg-emerald-950/70 border-emerald-500/50'
                          }`}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${colors.dot}`} />
                          {free} spaces available
                        </span>
                      </div>
                    </div>
                    <div className="w-full h-2 rounded-full bg-[#0d1320] overflow-hidden p-0.5 border border-[#222d42]/70">
                      <div
                        className={`h-full rounded-full transition-all duration-500 shadow-sm ${occupancyBarColor(s.level)}`}
                        style={{ width: `${Math.min(100, pct)}%` }}
                      />
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        </div>

        {/* Right: Tactical Geo-Spatial Map Command Viewport (Using the Leaflet Map) */}
        <div className="lg:col-span-6 xl:col-span-6 relative flex flex-col h-[560px] lg:h-[calc(100vh-240px)] min-h-[480px] rounded-xl overflow-hidden border border-[#222d42] bg-[#090e18] shadow-2xl">
  

          {/* Embedded Mapbox Map */}
          <div className="w-full h-full flex-1">
            <MapboxMap
              height="100%"
              label="Tactical map of shelters"
              darkTheme={false}
              markers={mapMarkers}
              selectedId={selectedShelterId ?? undefined}
              onMarkerClick={(m) => setSelectedShelterId(m.id)}
            />
          </div>

          {/* Bottom Floating Map Legend & Attribution */}
          <div className="absolute bottom-3 left-3 right-3 flex flex-col sm:flex-row items-center justify-between gap-3 pointer-events-auto z-[400] bg-[#0d1320]/90 backdrop-blur-md border border-[#222d42] rounded-lg px-4 py-2.5 shadow-2xl">
            <div className="flex items-center flex-wrap gap-3">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Map Legend:</span>
              <div className="flex items-center gap-2 flex-wrap">
                <div className="bg-emerald-950/60 border border-emerald-500/40 text-emerald-400 px-2 py-0.5 rounded text-[11px] font-medium flex items-center gap-1.5 shadow-sm">
                  <span className="w-2 h-2 rounded-full bg-emerald-400" />
                  <span>Normal (&lt;90%)</span>
                </div>
                <div className="bg-amber-500/20 border border-amber-500/50 text-amber-300 px-2 py-0.5 rounded text-[11px] font-medium flex items-center gap-1.5 shadow-sm">
                  <span className="w-2 h-2 rounded-full bg-amber-400" />
                  <span>Nearly full (≥90%)</span>
                </div>
                <div className="bg-rose-950/60 border border-rose-500/40 text-rose-300 px-2 py-0.5 rounded text-[11px] font-medium flex items-center gap-1.5 shadow-sm">
                  <span className="w-2 h-2 rounded-full bg-rose-500" />
                  <span>Full (100%)</span>
                </div>
              </div>
            </div>
            <div className="text-[11px] text-slate-400 tracking-tight shrink-0 select-none">
              <span>Leaflet | © OpenStreetMap contributors</span>
            </div>
          </div>
        </div>
      </section>

      {/* Headcount Modal Dialog */}
      {counting && <HeadcountDialog shelter={counting} onClose={() => setCounting(null)} />}
    </div>
  )
}
