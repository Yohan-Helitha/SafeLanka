import type { AlertTimelineSection as Data } from '@/types'
import { formatDateTime, formatMinutes } from '@/utils/format'
import { SeverityBadge, StatusChip } from '../domain'
import { Card, Stat } from '../ui'

export function AlertTimelineSection({ data, districtName }: { data: Data; districtName: (id: string) => string }) {
  return (
    <Card title="1. Alert timeline">
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <Stat label="First verified report" value={<span className="text-2xl">{formatDateTime(data.firstVerifiedReportAt)}</span>} />
        <Stat label="First warning" value={<span className="text-2xl">{formatDateTime(data.firstWarningAt)}</span>} />
        <Stat label="Report to warning" value={formatMinutes(data.minutesReportToWarning)} tone="signal" />
      </div>
      <ol className="relative ml-2 space-y-4 border-l border-line pl-5">
        {data.entries.map((e) => (
          <li key={e.warningId} className="relative">
            <span className="absolute -left-[27px] top-1.5 size-3 rounded-full bg-signal ring-4 ring-panel" aria-hidden />
            <div className="flex flex-wrap items-center gap-2">
              <SeverityBadge level={e.level} size="sm" />
              <span className="tabular text-sm text-muted">{formatDateTime(e.issuedAt)}</span>
              <StatusChip status={e.status} />
              {e.isEscalation && <span className="text-xs font-medium uppercase tracking-wide text-caution">escalation</span>}
            </div>
            <p className="mt-1 text-[15px] text-ink">{e.title}</p>
            <p className="text-sm text-muted">{e.districtIds.map(districtName).join(', ')}</p>
          </li>
        ))}
      </ol>
    </Card>
  )
}
