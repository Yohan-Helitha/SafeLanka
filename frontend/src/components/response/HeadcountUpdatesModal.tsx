import { useState } from 'react'
import {
  AlertTriangle,
  Building2,
  CheckCircle2,
  Clock,
  Sparkles,
  User,
  XCircle,
} from 'lucide-react'
import { useToast } from '@/context/ToastContext'
import {
  useApplyHeadcountUpdate,
  useDismissHeadcountUpdate,
  useHeadcountUpdates,
} from '@/hooks/response/useResponse'
import type { Shelter, ShelterHeadcountUpdate } from '@/types'
import { relativeTime } from '@/utils/format'
import { Dialog } from '../ui/Dialog'

interface Props {
  districtId: string
  allShelters: Shelter[]
  onClose: () => void
}

export function HeadcountUpdatesModal({
  districtId,
  allShelters,
  onClose,
}: Props) {

  const { toast } = useToast()
  const updatesQuery = useHeadcountUpdates({ districtId })
  const applyMutation = useApplyHeadcountUpdate()
  const dismissMutation = useDismissHeadcountUpdate()

  const [activeTab, setActiveTab] = useState<'UPDATES' | 'NEW_SHELTERS'>('UPDATES')
  const [editingUpdate, setEditingUpdate] = useState<ShelterHeadcountUpdate | null>(null)
  const [customCount, setCustomCount] = useState<number>(0)

  const updates = (updatesQuery.data ?? []) as ShelterHeadcountUpdate[]
  const pendingUpdates = updates.filter((u) => u.status === 'PENDING')
  const historyUpdates = updates.filter((u) => u.status !== 'PENDING')

  // Newly activated/added shelters (e.g. shelters added in seed / recent 4)
  const newlyAddedShelters = allShelters.slice(-4)

  const handleApply = (update: ShelterHeadcountUpdate, customVal?: number) => {
    applyMutation.mutate(
      {
        updateId: update.id,
        customOccupancy: customVal,
      },
      {
        onSuccess: () => {
          toast(
            `Headcount for ${update.shelterName} successfully updated to ${
              customVal ?? update.reportedOccupancy
            } evacuees!`,
            'good',
          )
          setEditingUpdate(null)
        },
        onError: (err: any) => {
          toast(err?.message || 'Failed to apply headcount update', 'bad')
        },
      },
    )
  }

  const handleDismiss = (update: ShelterHeadcountUpdate) => {
    dismissMutation.mutate(update.id, {
      onSuccess: () => {
        toast(`Headcount report for ${update.shelterName} dismissed.`, 'info')
      },
      onError: (err: any) => {
        toast(err?.message || 'Failed to dismiss update', 'bad')
      },
    })
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title="Shelter Headcount Updates & Alerts"
      description="Review field reports submitted by coordinators and volunteers to update shelter headcounts."
      className="max-w-4xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <span className="text-xs text-slate-400 font-mono">
            {pendingUpdates.length} pending report{pendingUpdates.length !== 1 ? 's' : ''}
          </span>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-300 hover:text-white hover:bg-[#1d273c] border border-[#2b3852] transition-colors"
          >
            Close
          </button>
        </div>
      }
    >
      <div className="space-y-4 py-1">
        {/* Navigation Tabs */}
        <div className="flex items-center gap-2 border-b border-[#222d42] pb-3">
          <button
            type="button"
            onClick={() => setActiveTab('UPDATES')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-2 ${
              activeTab === 'UPDATES'
                ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-[#141c2c]'
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            <span>Incoming Headcount Reports</span>
            {pendingUpdates.length > 0 && (
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/40 font-mono">
                {pendingUpdates.length}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('NEW_SHELTERS')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-2 ${
              activeTab === 'NEW_SHELTERS'
                ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-[#141c2c]'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
            <span>Newly Added Shelters ({newlyAddedShelters.length})</span>
          </button>
        </div>

        {/* Tab 1: Incoming Headcount Reports (Single clean scroll from dialog) */}
        {activeTab === 'UPDATES' && (
          <div className="space-y-4">
            {pendingUpdates.length === 0 ? (
              <div className="text-center py-10 rounded-xl border border-[#1b263b] bg-[#0c1322]">
                <CheckCircle2 className="w-10 h-10 text-emerald-400 mx-auto mb-2 opacity-80" />
                <h4 className="text-sm font-bold text-white">All Headcount Reports Processed</h4>
                <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                  No pending headcount updates from field coordinators. All shelters are up to date.
                </p>
              </div>
            ) : (
              pendingUpdates.map((item) => {
                const shelter = allShelters.find((s) => s.id === item.shelterId)
                const capacity = item.shelterCapacity || shelter?.capacity || 100
                const currentOcc = item.currentShelterOccupancy ?? shelter?.currentOccupancy ?? 0
                const reportedOcc = item.reportedOccupancy
                const delta = reportedOcc - currentOcc
                const pct = Math.min(100, Math.round((reportedOcc / capacity) * 100))
                const isOverCap = reportedOcc > capacity
                const isAmber = pct >= 85

                const isEditing = editingUpdate?.id === item.id

                return (
                  <article
                    key={item.id}
                    className="border border-[#22334e] bg-[#101b2d] rounded-xl p-4 shadow-md transition-all hover:border-cyan-500/40"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-3">
                      <div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <h3 className="text-sm font-bold text-white tracking-tight">
                            {item.shelterName}
                          </h3>
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#16233b] border border-[#263756] text-cyan-300">
                            Cap: {capacity}
                          </span>
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/40">
                            Pending Review
                          </span>
                        </div>
                        <div className="flex items-center gap-3 text-xs text-slate-400 mt-1">
                          <span className="flex items-center gap-1">
                            <User className="w-3 h-3 text-cyan-400" />
                            <strong className="text-slate-300 font-medium">
                              {item.reportedByName}
                            </strong>
                            <span className="text-[10px] text-slate-500">
                              ({item.reportedByRole})
                            </span>
                          </span>
                          <span className="flex items-center gap-1 font-mono text-[11px] text-slate-500">
                            <Clock className="w-3 h-3" />
                            {relativeTime(item.reportedAt)}
                          </span>
                        </div>
                      </div>

                      {/* Headcount Numbers Display */}
                      <div className="flex items-center gap-3 bg-[#0c1424] border border-[#1d2b44] rounded-lg px-3 py-1.5 self-start">
                        <div className="text-right">
                          <span className="text-[10px] uppercase font-bold text-slate-500 block">
                            Current
                          </span>
                          <span className="text-sm font-mono font-bold text-slate-400">
                            {currentOcc}
                          </span>
                        </div>
                        <span className="text-slate-600 font-bold">→</span>
                        <div>
                          <span className="text-[10px] uppercase font-bold text-cyan-400 block">
                            Reported
                          </span>
                          <div className="flex items-baseline gap-1">
                            <span className="text-base font-mono font-extrabold text-cyan-300">
                              {reportedOcc}
                            </span>
                            <span
                              className={`text-[11px] font-mono font-bold ${
                                delta >= 0 ? 'text-emerald-400' : 'text-rose-400'
                              }`}
                            >
                              ({delta >= 0 ? `+${delta}` : delta})
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Field Report Message Box */}
                    <div className="bg-[#0a1120] border border-[#1b2a44] rounded-lg p-2.5 mb-3 text-xs text-slate-300 flex items-start gap-2">
                      <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                      <div>
                        <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block">
                          Field Remarks:
                        </span>
                        <p className="text-xs text-slate-200 mt-0.5 leading-relaxed">
                          "{item.message}"
                        </p>
                      </div>
                    </div>

                    {/* Occupancy bar preview */}
                    <div className="mb-3">
                      <div className="flex items-center justify-between text-[11px] text-slate-400 mb-1">
                        <span>Anticipated Occupancy Level:</span>
                        <span
                          className={`font-semibold ${
                            isOverCap
                              ? 'text-rose-400'
                              : isAmber
                              ? 'text-amber-400'
                              : 'text-emerald-400'
                          }`}
                        >
                          {pct}% ({capacity - reportedOcc} spaces free)
                        </span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-[#0a101d] overflow-hidden border border-[#1b263b]">
                        <div
                          className={`h-full transition-all duration-300 ${
                            isOverCap
                              ? 'bg-rose-500'
                              : isAmber
                              ? 'bg-amber-400'
                              : 'bg-emerald-400'
                          }`}
                          style={{ width: `${Math.min(100, pct)}%` }}
                        />
                      </div>
                    </div>

                    {/* Action Buttons */}
                    {isEditing ? (
                      <div className="bg-[#0b1322] border border-cyan-500/40 rounded-lg p-3 flex flex-col sm:flex-row items-center gap-2">
                        <div className="flex-1 flex items-center gap-2">
                          <label className="text-xs text-slate-300 font-medium">
                            Adjust Headcount:
                          </label>
                          <input
                            type="number"
                            min="0"
                            max={capacity}
                            value={customCount}
                            onChange={(e) => setCustomCount(Number(e.target.value))}
                            className="w-24 px-2 py-1 rounded bg-[#101b2d] border border-[#22334e] text-white text-xs font-mono focus:border-cyan-400 focus:outline-none"
                          />
                          <span className="text-xs text-slate-500">/ {capacity}</span>
                        </div>
                        <div className="flex items-center gap-2 self-end sm:self-auto">
                          <button
                            type="button"
                            onClick={() => setEditingUpdate(null)}
                            className="px-2.5 py-1 text-xs text-slate-400 hover:text-white"
                          >
                            Cancel
                          </button>
                          <button
                            type="button"
                            disabled={applyMutation.isPending}
                            onClick={() => handleApply(item, customCount)}
                            className="px-3 py-1 rounded-md bg-cyan-600 hover:bg-cyan-500 text-xs font-bold text-white transition-all shadow-sm"
                          >
                            Save &amp; Update
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center justify-end gap-2 pt-1 border-t border-[#1a273e]">
                        <button
                          type="button"
                          disabled={dismissMutation.isPending}
                          onClick={() => handleDismiss(item)}
                          className="px-2.5 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-[#152033] transition-colors"
                        >
                          Dismiss
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setEditingUpdate(item)
                            setCustomCount(reportedOcc)
                          }}
                          className="px-3 py-1.5 rounded-lg border border-[#2b3b57] text-xs font-semibold text-slate-200 hover:bg-[#17243a] transition-colors"
                        >
                          Adjust Count
                        </button>
                        <button
                          type="button"
                          disabled={applyMutation.isPending}
                          onClick={() => handleApply(item)}
                          className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-xs font-bold text-white transition-all shadow-md flex items-center gap-1.5"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Apply Reported Count ({reportedOcc})
                        </button>
                      </div>
                    )}
                  </article>
                )
              })
            )}

            {/* History processed items */}
            {historyUpdates.length > 0 && (
              <div className="pt-4 border-t border-[#1e2a40]">
                <span className="text-[11px] uppercase font-bold tracking-wider text-slate-500 block mb-2">
                  Recently Processed Updates ({historyUpdates.length})
                </span>
                <div className="space-y-2">
                  {historyUpdates.slice(0, 3).map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between p-2.5 rounded-lg bg-[#0c1424] border border-[#19263c] text-xs text-slate-400"
                    >
                      <div className="flex items-center gap-2">
                        {item.status === 'APPLIED' ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                        ) : (
                          <XCircle className="w-4 h-4 text-slate-500" />
                        )}
                        <span className="font-semibold text-slate-200">{item.shelterName}</span>
                        <span>•</span>
                        <span>
                          {item.reportedOccupancy} occupants ({item.status})
                        </span>
                      </div>
                      <span className="text-[10px] font-mono text-slate-500">
                        {relativeTime(item.reportedAt)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab 2: Newly Added Emergency Shelters */}
        {activeTab === 'NEW_SHELTERS' && (
          <div className="space-y-3">
            <p className="text-xs text-slate-400 mb-2">
              Recently commissioned emergency centers registered in this district with verification metadata and commissioning authority.
            </p>
            {newlyAddedShelters.map((s) => {
              const occPct = Math.min(100, Math.round((s.currentOccupancy / s.capacity) * 100))
              const dateText = s.createdAt
                ? `${relativeTime(s.createdAt)} (${new Date(s.createdAt).toLocaleDateString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })})`
                : 'Recently registered'
              const addedByText = s.addedBy || 'Disaster Operations Command'

              return (
                <div
                  key={s.id}
                  className="border border-[#22334e] bg-[#101b2d] rounded-xl p-4 flex flex-col gap-3 shadow-md"
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-2.5">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <Building2 className="w-4 h-4 text-cyan-400" />
                        <h4 className="text-sm font-bold text-white tracking-tight">{s.name}</h4>
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-950/80 text-emerald-400 border border-emerald-500/40">
                          {s.status}
                        </span>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#16233b] border border-[#263756] text-cyan-300">
                          Capacity: {s.capacity}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400">{s.address}</p>
                    </div>

                    <div className="flex items-center gap-2 text-xs font-mono text-slate-300 bg-[#0c1424] border border-[#1e2c44] rounded-lg px-3 py-1.5 self-start">
                      <span className="text-slate-400">Occupancy:</span>
                      <strong className="text-white">{s.currentOccupancy}</strong> / {s.capacity}
                      <span className="text-cyan-400 font-semibold">({occPct}%)</span>
                    </div>
                  </div>

                  {/* Metadata Row: Date added & Who added the shelter */}
                  <div className="pt-2.5 border-t border-[#1a273e] flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                    <div className="flex items-center gap-2 text-slate-300">
                      <User className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                      <span>
                        <span className="text-slate-400">Added by:</span>{' '}
                        <strong className="text-cyan-200 font-semibold">{addedByText}</strong>
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 text-slate-400 font-mono text-[11px]">
                      <Clock className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                      <span>
                        <span className="text-slate-500">Date Added:</span> {dateText}
                      </span>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}

      </div>
    </Dialog>
  )
}
