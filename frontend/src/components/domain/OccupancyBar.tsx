import clsx from 'clsx'
import { THRESHOLDS } from '@/theme/tokens'
import { formatPercent } from '@/utils/format'

interface Props {
  occupancy: number
  capacity: number
  showCount?: boolean
}

/** Green below 90 %, amber from 90 %, red when full. */
export function OccupancyBar({ occupancy, capacity, showCount = true }: Props) {
  const ratio = capacity ? Math.min(1, occupancy / capacity) : 0
  const full = occupancy >= capacity
  const amber = !full && ratio >= THRESHOLDS.shelterAmber
  const label = full ? 'Full' : amber ? `Nearly full · ${formatPercent(ratio)}` : formatPercent(ratio)
  return (
    <div>
      <div
        role="meter"
        aria-label="Shelter occupancy"
        aria-valuemin={0}
        aria-valuemax={capacity}
        aria-valuenow={occupancy}
        aria-valuetext={`${occupancy} of ${capacity}, ${label}`}
        className="h-2.5 overflow-hidden rounded-full bg-line"
      >
        <div
          className={clsx('h-full rounded-full', full ? 'bg-danger' : amber ? 'bg-caution' : 'bg-ok')}
          style={{ width: `${ratio * 100}%` }}
        />
      </div>
      <p className="tabular mt-1 text-sm text-muted">
        {showCount && `${occupancy}/${capacity} · `}
        <span className={clsx(full && 'font-medium text-danger', amber && 'font-medium text-caution')}>{label}</span>
      </p>
    </div>
  )
}
