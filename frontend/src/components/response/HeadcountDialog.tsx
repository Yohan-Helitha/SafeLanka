import { useEffect, useRef, useState } from 'react'
import { Save } from 'lucide-react'
import { useToast } from '@/context/ToastContext'
import { useUpdateOccupancy } from '@/hooks/response/useResponse'
import type { Shelter } from '@/types'
import { ApiErrorNotice } from '../domain'
import { Dialog } from '../ui/Dialog'

const CIRCLE_CIRCUMFERENCE = 2 * Math.PI * 82
const WARNING_THRESHOLD = 0.9

export function HeadcountDialog({ shelter, onClose }: { shelter: Shelter; onClose: () => void }) {
  const { toast } = useToast()
  const update = useUpdateOccupancy()
  const [count, setCount] = useState(shelter.currentOccupancy)
  const over = count > shelter.capacity
  const progressRef = useRef<SVGCircleElement>(null)
  const badgeRef = useRef<HTMLSpanElement>(null)
  const countDisplayRef = useRef<HTMLSpanElement>(null)
  const radialPercentRef = useRef<HTMLSpanElement>(null)
  const remainingRef = useRef<HTMLSpanElement>(null)

  const pct = shelter.capacity > 0 ? Math.min(1, count / shelter.capacity) : 0
  const remaining = shelter.capacity - count
  const isWarning = pct >= WARNING_THRESHOLD

  useEffect(() => {
    if (!progressRef.current || !badgeRef.current || !countDisplayRef.current || !radialPercentRef.current || !remainingRef.current) return
    const offset = CIRCLE_CIRCUMFERENCE - pct * CIRCLE_CIRCUMFERENCE
    progressRef.current.style.strokeDashoffset = String(offset)
    progressRef.current.setAttribute('stroke', isWarning ? '#f59e0b' : '#10b981')
    badgeRef.current.className = `text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded border ${
      isWarning ? 'bg-amber-500/15 text-amber-400 border-amber-500/30' : 'bg-cyan-500/15 text-cyan-400 border-cyan-500/30'
    }`
    badgeRef.current.textContent = isWarning ? 'Near Capacity' : 'Optimal / Safe'
    countDisplayRef.current.textContent = String(count)
    radialPercentRef.current.textContent = `${Math.round(pct * 100)}%`
    remainingRef.current.textContent = `${remaining} spaces free`
  }, [pct, isWarning, count, remaining])

  const handleSave = () => {
    update.mutate(
      { shelterId: shelter.id, occupancy: count },
      {
        onSuccess: () => {
          toast('Headcount updated')
          onClose()
        },
      }
    )
  }

  const increment = () => setCount((c) => Math.min(shelter.capacity, c + 1))
  const decrement = () => setCount((c) => Math.max(0, c - 1))

  return (
    <Dialog
      open
      onClose={onClose}
      title="Update Shelter Headcount"
      description={
        <span>
          <span className="text-slate-200 font-semibold">{shelter.name}</span> holds up to{' '}
          <span className="font-mono text-slate-300">{shelter.capacity}</span> evacuees.
        </span>
      }
      className="max-w-5xl"
      footer={
        <>
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl font-medium text-sm text-slate-300 hover:text-white hover:bg-[#1d273c] border border-transparent hover:border-[#32425f] transition-colors duration-150"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={over || count < 0 || update.isPending}
            className="px-6 py-2.5 rounded-xl font-semibold text-sm text-slate-950 bg-gradient-to-r from-cyan-400 to-cyan-500 hover:from-cyan-300 hover:to-cyan-400 active:scale-95 shadow-lg shadow-cyan-500/20 transition-all duration-150 flex items-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {update.isPending ? (
              <>
                <svg className="animate-spin w-4 h-4 text-slate-950" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                </svg>
                Saving...
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                Save Headcount
              </>
            )}
          </button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[420px]">
        {/* Left Panel: Radial Occupancy Meter */}
        <section aria-label="Occupancy Telemetry" className="lg:col-span-5 p-6 bg-slate-900/40 border-b lg:border-b-0 lg:border-r border-[#243046] flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-mono tracking-wider uppercase text-slate-400">Shelter Occupancy</span>
              <span ref={badgeRef} className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded border bg-cyan-500/15 text-cyan-400 border-cyan-500/30">
                Optimal / Safe
              </span>
            </div>

            {/* Circular Radial Gauge */}
            <div className="relative flex items-center justify-center my-4 py-2">
              <svg className="w-52 h-52" viewBox="0 0 200 200">
                <circle cx="100" cy="100" fill="none" r="82" stroke="#243046" strokeWidth="14" />
                <circle cx="100" cy="100" fill="none" opacity="0.25" r="82" stroke="#f59e0b" strokeDasharray="51.5 515.2" strokeDashoffset="-463.6" strokeWidth="14" />
                <circle
                  ref={progressRef}
                  className="capacity-ring-circle"
                  cx="100"
                  cy="100"
                  fill="none"
                  r="82"
                  stroke="#10b981"
                  strokeDasharray="515.2"
                  strokeDashoffset={CIRCLE_CIRCUMFERENCE - pct * CIRCLE_CIRCUMFERENCE}
                  strokeLinecap="round"
                  strokeWidth="14"
                  style={{ strokeDashoffset: CIRCLE_CIRCUMFERENCE - pct * CIRCLE_CIRCUMFERENCE }}
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                <span ref={radialPercentRef} className="font-mono text-3xl font-extrabold text-white tracking-tight">
                  {Math.round(pct * 100)}%
                </span>
                <span className="text-xs uppercase tracking-widest text-slate-400 mt-0.5">Capacity</span>
                <div className="mt-2 text-xs font-mono text-slate-400">
                  <span ref={countDisplayRef} className="text-white font-bold text-sm">{count}</span> / {shelter.capacity}
                </div>
              </div>
            </div>

            {/* Available Space */}
            <div className="bg-[#1d273c]/60 border border-[#243046]/80 rounded-xl p-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="h-2 w-2 rounded-full bg-cyan-400" />
                <span className="text-xs text-slate-300 font-medium">Available Space</span>
              </div>
              <span ref={remainingRef} className="font-mono text-xs font-semibold text-cyan-400 bg-cyan-500/10 px-2 py-0.5 rounded border border-cyan-500/20">
                {remaining} spaces free
              </span>
            </div>
          </div>
        </section>

        {/* Right Panel: Controls */}
        <section aria-label="Headcount Controls" className="lg:col-span-7 p-6 flex flex-col justify-between bg-[#151d2d]">
          <div className="space-y-6">
            {/* Stepper */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-mono uppercase tracking-wider text-slate-400 font-semibold" htmlFor="headcount-slider">
                  Direct Count Adjustment
                </label>
              </div>

              {/* Stepper Controls */}
              <div className="flex items-center justify-between bg-slate-900/90 border border-[#243046] rounded-xl p-2 shadow-inner">
                <button
                  type="button"
                  onClick={decrement}
                  className="w-14 h-14 rounded-lg bg-[#1d273c] hover:bg-slate-700/80 active:scale-95 border border-[#243046] text-slate-200 hover:text-white flex items-center justify-center transition-all duration-100 shadow"
                  aria-label="Decrease headcount by 1"
                >
                  <svg className="w-6 h-6 stroke-[2.5]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path d="M20 12H4" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </button>

                <div className="text-center px-4">
                  <div className="relative inline-block">
                    <span ref={countDisplayRef} className="font-mono text-5xl font-black text-white tracking-tight drop-shadow-sm">
                      {count}
                    </span>
                  </div>
                  <div className="text-[11px] uppercase font-mono tracking-widest text-slate-400 mt-1">Sheltered People</div>
                </div>

                <button
                  type="button"
                  onClick={increment}
                  className="w-14 h-14 rounded-lg bg-[#1d273c] hover:bg-slate-700/80 active:scale-95 border border-[#243046] text-slate-200 hover:text-white flex items-center justify-center transition-all duration-100 shadow"
                  aria-label="Increase headcount by 1"
                >
                  <svg className="w-6 h-6 stroke-[2.5]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path d="M12 4v16m8-8H4" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Range Slider */}
            <div className="space-y-1.5 pt-1">
              <div className="flex justify-between items-center text-xs font-mono text-slate-400">
                <span>0 (Empty)</span>
                <span>{shelter.capacity} (Full)</span>
              </div>
              <input
                id="headcount-slider"
                type="range"
                min={0}
                max={shelter.capacity}
                value={count}
                onChange={(e) => setCount(parseInt(e.target.value, 10))}
                className="w-full h-2 bg-slate-900/90 rounded-lg appearance-none cursor-pointer border border-[#243046]"
                style={{
                  background: `linear-gradient(to right, #06b6d4 0%, #06b6d4 ${pct * 100}%, #0f172a ${pct * 100}%, #0f172a 100%)`,
                }}
              />
            </div>
          </div>

          {/* Footer */}
          <div className="mt-6 pt-4 border-t border-[#243046]/60 flex items-center justify-between">
            <div className="flex items-center gap-2 text-[11px] text-slate-400">
              <svg className="w-4 h-4 text-cyan-400 flex-shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>Updates push directly to the Colombo District Disaster Ledger.</span>
            </div>
          </div>
        </section>
      </div>

      {over && (
        <div className="px-6 py-3 bg-amber-500/10 border-t border-amber-500/30">
          <p className="text-sm text-amber-400">
            That is {count - shelter.capacity} more than the shelter holds. Lower the number or move people to another shelter.
          </p>
        </div>
      )}
      <ApiErrorNotice error={update.error} />
    </Dialog>
  )
}
