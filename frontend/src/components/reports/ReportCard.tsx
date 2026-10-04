import { Image } from 'lucide-react'
import { CATEGORY_LABEL, CITIZEN_REPORT_STATUS, REJECTION_LABEL } from '@/constants/labels'
import type { ReportListItem } from '@/types'
import { relativeTime } from '@/utils/format'
import { StatusChip } from '../domain'

interface Props {
  report: ReportListItem
  hazardTypeName: string
}

/** A report as the person who sent it sees it, in citizen wording. */
export function ReportCard({ report, hazardTypeName }: Props) {
  return (
    <article className="rounded-card border border-line bg-panel p-4">
      <header className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold text-ink">
            {hazardTypeName} · {CATEGORY_LABEL[report.category] ?? report.category}
          </h3>
          <p className="tabular text-sm text-muted">
            {report.referenceNo} · {relativeTime(report.capturedAt)}
          </p>
        </div>
        <StatusChip status={report.status} label={CITIZEN_REPORT_STATUS[report.status]} />
      </header>
      <p className="mt-2 line-clamp-2 text-[15px] text-ink">{report.description}</p>
      {report.hasPhoto && (
        <p className="mt-2 flex items-center gap-1.5 text-sm text-muted">
          <Image className="size-4" aria-hidden /> Photo attached
        </p>
      )}
      {report.status === 'REJECTED' && report.rejectionReason && (
        <div className="mt-3 rounded-control bg-raised p-3 text-sm">
          <p className="font-medium text-ink">{REJECTION_LABEL[report.rejectionReason]}</p>
          {report.reviewComment && <p className="mt-0.5 text-muted">{report.reviewComment}</p>}
        </div>
      )}
      {report.status === 'NEEDS_MORE_INFO' && (
        <div className="mt-3 rounded-control border border-caution/50 bg-caution/10 p-3 text-sm text-ink">
          The officer asked: {report.reviewComment}
        </div>
      )}
    </article>
  )
}
