import { MapPin } from 'lucide-react'
import type { ReactNode } from 'react'
import type { Shelter } from '@/types'
import { OccupancyBar, StatusChip } from '../domain'

interface Props {
  shelter: Shelter
  districtName?: string
  actions?: ReactNode
  size?: 'md' | 'lg'
}

export function ShelterCard({ shelter, districtName, actions, size = 'md' }: Props) {
  return (
    <article className="rounded-card border border-line bg-panel p-4">
      <header className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className={size === 'lg' ? 'font-display text-xl font-semibold text-ink' : 'font-semibold text-ink'}>{shelter.name}</h3>
          <p className="mt-0.5 flex items-center gap-1 text-sm text-muted">
            <MapPin className="size-3.5" aria-hidden />
            {districtName ? `${shelter.address}, ${districtName}` : shelter.address}
          </p>
        </div>
        <StatusChip status={shelter.status} />
      </header>
      <div className="mt-3">
        <OccupancyBar occupancy={shelter.currentOccupancy} capacity={shelter.capacity} />
      </div>
      {actions && <div className="mt-3">{actions}</div>}
    </article>
  )
}
