import { Clock, MapPin, Navigation, Users } from 'lucide-react'
import type { ReactNode } from 'react'
import { PRIORITY_LABEL } from '@/constants/labels'
import type { Assignment } from '@/types'
import { formatCoords, relativeTime } from '@/utils/format'
import { directionsLink } from '@/utils/geo'
import { StatusChip } from '../domain'
import { LocationMap } from '../ui'

/** The task, place, people and destination of an assignment, for rescue members. */
export function AssignmentSummary({ assignment: a, status, footer }: { assignment: Assignment; status: string; footer?: ReactNode }) {
  return (
    <article className="rounded-card border border-line bg-panel p-4">
      <header className="flex items-center justify-between gap-3">
        <p className={a.priority === 1 ? 'font-semibold text-danger' : 'font-semibold text-ink'}>
          {a.priority === 1 ? 'Urgent assignment' : `${PRIORITY_LABEL[a.priority]} assignment`}
        </p>
        <StatusChip status={status} />
      </header>
      <p className="mt-3 text-lg font-semibold text-ink">{a.task}</p>
      <ul className="mt-3 space-y-2 text-[15px] text-ink">
        <li className="flex gap-2">
          <MapPin className="mt-0.5 size-4 shrink-0 text-muted" aria-hidden />
          <span>
            {a.locationText}
            <span className="tabular block text-sm text-muted">{formatCoords(a.latitude, a.longitude)}</span>
          </span>
        </li>
        <li className="flex gap-2">
          <Users className="mt-0.5 size-4 shrink-0 text-muted" aria-hidden />
          About {a.peopleEstimated} people
        </li>
        {a.destinationShelterName && (
          <li className="flex gap-2">
            <Navigation className="mt-0.5 size-4 shrink-0 text-muted" aria-hidden />
            Take them to {a.destinationShelterName}
          </li>
        )}
        {a.assignedAt && (
          <li className="flex gap-2">
            <Clock className="mt-0.5 size-4 shrink-0 text-muted" aria-hidden />
            Assigned {relativeTime(a.assignedAt)}
          </li>
        )}
      </ul>
      <div className="mt-3">
        <LocationMap height={180} label={`Map of ${a.locationText}`} markers={[{ id: a.id, latitude: a.latitude, longitude: a.longitude, label: a.locationText, tone: a.priority === 1 ? 'danger' : 'signal' }]} />
      </div>
      <a
        className="mt-3 inline-block text-sm font-medium text-signal hover:underline"
        href={directionsLink({ latitude: a.latitude, longitude: a.longitude })}
        target="_blank"
        rel="noreferrer"
      >
        Open directions
      </a>
      {footer && <div className="mt-4">{footer}</div>}
    </article>
  )
}
