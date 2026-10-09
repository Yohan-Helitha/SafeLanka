import { useMemo, useState } from 'react'
import { Building2, Package } from 'lucide-react'
import { AllocateDialog } from '@/components/response/AllocateDialog'
import { ErrorState, Loading } from '@/components/ui'
import { useCurrentUser } from '@/context/AuthContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAllocations, useShelters, useStocks } from '@/hooks/response/useResponse'
import type { Allocation, ReliefStock } from '@/types'
import { formatNumber, relativeTime } from '@/utils/format'

export function ReliefSuppliesScreen() {
  useDocumentTitle('Relief supplies')
  const user = useCurrentUser()
  const { activeEvents, reliefItems, organisations } = useReferenceData()
  const stocks = useStocks(user.districtId)
  const allStocks = useStocks()
  const allocations = useAllocations({ districtId: user.districtId })
  const shelters = useShelters({ districtId: user.districtId })
  const allShelters = useShelters({})
  const [allocating, setAllocating] = useState<ReliefStock | null>(null)
  const event = activeEvents.find((e) => e.districtIds.includes(user.districtId)) ?? activeEvents[0]

  const getItemName = (s: ReliefStock) => s.itemName || reliefItems.find((ri) => ri.id === s.itemId)?.name || 'Relief Item'
  const getOrgName = (s: ReliefStock) => s.organisationName || organisations.find((o) => o.id === s.organisationId)?.name || 'DMC'
  const getUnit = (s: ReliefStock) => s.unit || reliefItems.find((ri) => ri.id === s.itemId)?.unit || 'units'

  const stocksList: ReliefStock[] = useMemo(
    () => (Array.isArray(stocks.data) ? (stocks.data as ReliefStock[]) : []),
    [stocks.data],
  )
  const allocationsList: Allocation[] = useMemo(
    () =>
      Array.isArray(allocations.data)
        ? (allocations.data as Allocation[])
        : Array.isArray((allocations.data as any)?.content)
        ? ((allocations.data as any).content as Allocation[])
        : [],
    [allocations.data],
  )

  const shelterMap = useMemo(() => {
    const map = new Map<string, string>()
    if (Array.isArray(allShelters.data)) {
      allShelters.data.forEach((s) => map.set(s.id, s.name))
    }
    if (Array.isArray(shelters.data)) {
      shelters.data.forEach((s) => map.set(s.id, s.name))
    }
    return map
  }, [shelters.data, allShelters.data])

  const stockMap = useMemo(() => {
    const map = new Map<string, ReliefStock>()
    if (Array.isArray(allStocks.data)) {
      allStocks.data.forEach((s) => map.set(s.id, s))
    }
    stocksList.forEach((s) => map.set(s.id, s))
    return map
  }, [stocksList, allStocks.data])

  const enrichedAllocations = useMemo<Allocation[]>(() => {
    return allocationsList.map((a) => {
      const matchedShelterName =
        a.shelterName || (a.shelterId ? shelterMap.get(a.shelterId) : null) || 'Shelter'

      const matchedStock = a.stockId ? stockMap.get(a.stockId) : undefined
      const matchedItemName =
        a.itemName ||
        matchedStock?.itemName ||
        (matchedStock?.itemId ? reliefItems.find((ri) => ri.id === matchedStock.itemId)?.name : null) ||
        'Relief supply'

      const matchedUnit =
        a.unit ||
        matchedStock?.unit ||
        (matchedStock?.itemId ? reliefItems.find((ri) => ri.id === matchedStock.itemId)?.unit : null) ||
        'units'

      const matchedOrg =
        a.organisationName ||
        matchedStock?.organisationName ||
        (matchedStock?.organisationId ? organisations.find((o) => o.id === matchedStock.organisationId)?.name : null) ||
        'DMC'

      return {
        ...a,
        shelterName: matchedShelterName,
        itemName: matchedItemName,
        unit: matchedUnit,
        organisationName: matchedOrg,
      } as Allocation
    })
  }, [allocationsList, shelterMap, stockMap, reliefItems, organisations])

  const totalStockItems = stocksList.length
  const activeAllocations = enrichedAllocations.filter(
    (a) => a.status === 'ALLOCATED' || a.status === 'PARTIALLY_DISTRIBUTED'
  ).length

  const allocationStatusStyles: Record<string, { bg: string; text: string; border: string; dot: string; label: string }> = {
    ALLOCATED: {
      bg: 'bg-[#0e2a3b]',
      text: 'text-[#38bdf8]',
      border: 'border-[#0284c7]/40',
      dot: 'bg-[#38bdf8]',
      label: 'Allocated',
    },
    PARTIALLY_DISTRIBUTED: {
      bg: 'bg-amber-950/40',
      text: 'text-amber-300',
      border: 'border-amber-500/40',
      dot: 'bg-amber-400',
      label: 'Partially distributed',
    },
    DISTRIBUTED: {
      bg: 'bg-emerald-950/40',
      text: 'text-emerald-300',
      border: 'border-emerald-500/40',
      dot: 'bg-emerald-400',
      label: 'Distributed',
    },
    CANCELLED: {
      bg: 'bg-slate-800',
      text: 'text-slate-300',
      border: 'border-slate-700',
      dot: 'bg-slate-500',
      label: 'Cancelled',
    },
  }

  const itemColors = ['bg-[#06b6d4]', 'bg-blue-400', 'bg-rose-400', 'bg-emerald-400', 'bg-amber-400', 'bg-violet-400']

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Page Header */}
      <section className="space-y-3 sm:space-y-4" data-purpose="page-header">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4">
          <div className="space-y-1">
            <h1 className="font-display text-2xl font-semibold leading-tight text-ink sm:text-[28px]">Relief supplies</h1>
            <p className="text-xs sm:text-sm text-slate-400">Stock held in your district, and what has been sent to shelters.</p>
          </div>
          <div className="flex flex-wrap items-center gap-2 sm:gap-3 w-full sm:w-auto">
            <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-[#161b29] border border-[#222d42]">
              <span className="text-xs text-slate-400 font-medium">Total Stock:</span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-[#0e2a3b] text-[#06b6d4] border border-[#06b6d4]/20">
                {totalStockItems} items
              </span>
            </div>
            <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-[#161b29] border border-[#222d42]">
              <span className="text-xs text-slate-400 font-medium">Active:</span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                {activeAllocations} in progress
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* Two-Column Grid Layout */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 sm:gap-6 items-start">
        {/* Left Column: Stock Inventory */}
        <div className="xl:col-span-7">
          <section className="bg-[#161b29] border border-[#222d42] rounded-xl shadow-lg overflow-hidden" data-purpose="stock-inventory">
            <div className="px-4 sm:px-6 py-3.5 sm:py-4 border-b border-[#222d42] flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <h3 className="text-sm sm:text-base font-semibold text-white tracking-wide">Stock Inventory</h3>
                <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-[#0e2a3b] text-[#06b6d4] border border-[#06b6d4]/20">
                  {totalStockItems} lines
                </span>
              </div>
            </div>

            {stocks.isLoading && (
              <div className="py-8">
                <Loading />
              </div>
            )}
            {stocks.isError && (
              <div className="p-4">
                <ErrorState error={stocks.error} onRetry={() => void stocks.refetch()} />
              </div>
            )}
            {!stocks.isLoading && !stocks.isError && stocksList.length === 0 && (
              <p className="py-8 text-center text-sm text-muted">No stock is held in this district.</p>
            )}

            {/* Mobile Card View (< sm) */}
            <div className="sm:hidden divide-y divide-[#222d42]/60">
              {stocksList.map((s, idx) => (
                <div key={s.id} className="p-3.5 space-y-2.5">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center space-x-2.5 min-w-0">
                      <span className={`w-2 h-2 rounded-full shrink-0 ${itemColors[idx % itemColors.length]}`} />
                      <span className="font-semibold text-sm text-white break-words">{getItemName(s)}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setAllocating(s)}
                      className="shrink-0 inline-flex items-center justify-center px-3 py-1 text-xs font-semibold rounded-md text-white bg-[#0e2a3b] hover:bg-[#06b6d4] hover:text-[#0a0f1a] border border-[#06b6d4]/40 active:scale-95 transition-all shadow-sm"
                    >
                      Allocate
                    </button>
                  </div>
                  <div className="flex items-center justify-between text-xs text-slate-400 pt-1 border-t border-[#222d42]/40">
                    <span className="truncate pr-2">{getOrgName(s)}</span>
                    <span className="font-semibold text-slate-100 shrink-0">
                      {formatNumber(s.quantityAvailable)} {getUnit(s)}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Tablet & Desktop Table View (>= sm) */}
            <div className="hidden sm:block overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-[#222d42]/80 bg-[#121622] text-xs font-semibold uppercase tracking-wider text-slate-400">
                    <th className="py-3 px-4 sm:px-6" scope="col">
                      Item
                    </th>
                    <th className="py-3 px-4 sm:px-6" scope="col">
                      Owner
                    </th>
                    <th className="py-3 px-4 sm:px-6" scope="col">
                      Available
                    </th>
                    <th className="py-3 px-4 sm:px-6 text-right" scope="col">
                      Action
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#222d42]/60">
                  {stocksList.map((s, idx) => (
                    <tr key={s.id} className="hover:bg-[#1a2133] transition-colors group">
                      <td className="py-3.5 px-4 sm:px-6 font-medium text-white">
                        <div className="flex items-center space-x-3">
                          <span className={`w-2 h-2 rounded-full shrink-0 ${itemColors[idx % itemColors.length]}`} />
                          <span className="truncate">{getItemName(s)}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 sm:px-6 text-slate-300">{getOrgName(s)}</td>
                      <td className="py-3.5 px-4 sm:px-6 font-semibold text-slate-100">
                        {formatNumber(s.quantityAvailable)} {getUnit(s)}
                      </td>
                      <td className="py-3.5 px-4 sm:px-6 text-right">
                        <button
                          type="button"
                          onClick={() => setAllocating(s)}
                          className="inline-flex items-center justify-center px-4 py-1.5 text-xs font-semibold rounded-md text-white bg-[#0e2a3b] hover:bg-[#06b6d4] hover:text-[#0a0f1a] border border-[#06b6d4]/40 transition-all duration-150 active:scale-95 shadow-sm"
                        >
                          Allocate
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        {/* Right Column: Allocations Tracker */}
        <div className="xl:col-span-5 space-y-4">
          <section className="bg-[#161b29] border border-[#222d42] rounded-xl shadow-lg p-4 sm:p-5" data-purpose="shelter-allocations">
            <div className="flex items-center justify-between pb-3 sm:pb-4 border-b border-[#222d42]">
              <div className="flex items-center space-x-2">
                <h3 className="text-sm sm:text-base font-semibold text-white tracking-wide">Allocations</h3>
                <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                  Active distribution
                </span>
              </div>
            </div>
            <div className="pt-3 sm:pt-4 space-y-3 sm:space-y-4">
              {allocations.isLoading && <Loading />}
              {allocations.isError && (
                <div className="py-4">
                  <ErrorState error={allocations.error} onRetry={() => void allocations.refetch()} />
                </div>
              )}
              {!allocations.isLoading && !allocations.isError && enrichedAllocations.length === 0 && (
                <p className="py-6 text-center text-sm text-muted">Nothing has been allocated yet.</p>
              )}
              {enrichedAllocations.map((a) => {
                const statusStyle = allocationStatusStyles[a.status] ?? allocationStatusStyles.ALLOCATED

                return (
                  <div
                    key={a.id}
                    className="p-3.5 sm:p-4 rounded-lg bg-[#121622] border border-[#222d42] space-y-2.5 sm:space-y-3 hover:border-[#06b6d4]/40 transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <Building2 className="w-4 h-4 text-cyan-400 shrink-0" />
                          <h4 className="text-sm font-bold text-white break-words">{a.shelterName}</h4>
                        </div>
                        <p className="text-xs text-slate-300 mt-1 flex items-center gap-1.5 pl-5.5">
                          <Package className="w-3.5 h-3.5 text-cyan-400/80 shrink-0" />
                          <span className="font-semibold text-cyan-300">{a.itemName}</span>
                          {a.organisationName && (
                            <span className="text-[11px] text-slate-400">· {a.organisationName}</span>
                          )}
                        </p>
                      </div>
                      <span
                        className={`inline-flex items-center px-2 py-0.5 sm:px-2.5 sm:py-1 rounded-full text-xs font-medium ${statusStyle.bg} ${statusStyle.text} border ${statusStyle.border} shadow-sm shrink-0`}
                      >
                        <span className={`w-1.5 h-1.5 mr-1.5 rounded-full ${statusStyle.dot}`} />
                        {statusStyle.label}
                      </span>
                    </div>
                    <div className="pt-2 border-t border-[#222d42]/70 flex items-center justify-between text-[11px] text-slate-400">
                      <span>Allocated: {relativeTime(a.allocatedAt)}</span>
                      <span className="text-[11px] font-mono text-slate-400">
                        {a.organisationName}
                      </span>
                    </div>
                  </div>
                )
              })}
            </div>
          </section>
        </div>
      </div>

      {allocating && event && <AllocateDialog stock={allocating} districtId={user.districtId} eventId={event.id} onClose={() => setAllocating(null)} />}
    </div>
  )
}
