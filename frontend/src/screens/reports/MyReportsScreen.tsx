import { CloudOff, Inbox } from 'lucide-react'
import { Link } from 'react-router-dom'
import { StatusChip } from '@/components/domain'
import { ReportCard } from '@/components/reports/ReportCard'
import { Button, EmptyState, ErrorState, Loading, PageHeader } from '@/components/ui'
import { CATEGORY_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useOutbox } from '@/context/OutboxContext'
import { useMyReports } from '@/hooks/reports/useReports'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { relativeTime } from '@/utils/format'

export function MyReportsScreen() {
  useDocumentTitle('My reports')
  const reports = useMyReports()
  const { hazardTypeName } = useReferenceData()
  const { items, discard } = useOutbox()
  const queued = items.filter((i) => i.payload.kind === 'REPORT')

  return (
    <div>
      <PageHeader title="My reports" subtitle="Everything you have sent to the Disaster Management Centre." />
      <div className="space-y-3">
        {queued.map((item) => {
          const input = item.payload.kind === 'REPORT' ? item.payload.input : null
          const bad = item.state === 'NEEDS_ATTENTION'
          return (
            <article
              key={item.clientRef}
              className={`rounded-card border bg-panel p-4 ${bad ? 'border-danger/60' : 'border-caution/60'}`}
            >
              <header className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="flex items-center gap-1.5 font-semibold text-ink">
                    <CloudOff className="size-4 text-caution" aria-hidden />
                    {input ? `${hazardTypeName(input.hazardTypeId)} · ${CATEGORY_LABEL[input.category] ?? input.category}` : item.label}
                  </h3>
                  <p className="text-sm text-muted">Saved {relativeTime(item.createdAt)}</p>
                </div>
                {bad ? <StatusChip status="NEEDS_ATTENTION" label="Could not send" /> : <StatusChip status="PENDING_SYNC" label="Saved on this device" />}
              </header>
              {input && <p className="mt-2 line-clamp-2 text-[15px] text-ink">{input.description}</p>}
              {bad && (
                <div className="mt-3 space-y-2">
                  <p className="text-sm text-danger">{item.lastError}</p>
                  <Button variant="secondary" size="sm" onClick={() => void discard(item.clientRef)}>
                    Remove
                  </Button>
                </div>
              )}
            </article>
          )
        })}

        {reports.isLoading && <Loading />}
        {reports.isError && <ErrorState error={reports.error} onRetry={() => void reports.refetch()} />}
        {reports.data?.map((r) => (
          <ReportCard key={r.id} report={r} hazardTypeName={hazardTypeName(r.hazardTypeId)} />
        ))}
        {reports.data?.length === 0 && queued.length === 0 && (
          <EmptyState
            icon={Inbox}
            title="No reports yet"
            description="If you see rising water, a blocked road or a landslide crack, tell us."
            action={
              <Link to={paths.citizen.report}>
                <Button>Report a hazard</Button>
              </Link>
            }
          />
        )}
      </div>
    </div>
  )
}
