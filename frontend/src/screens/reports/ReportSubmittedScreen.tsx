import { CheckCircle2, CloudOff } from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { StatusChip } from '@/components/domain'
import { Button } from '@/components/ui'
import { paths } from '@/constants/routes'
import { useMyReports } from '@/hooks/reports/useReports'
import { useDocumentTitle } from '@/hooks/shared'

interface NavState {
  kind?: 'sent' | 'queued'
  referenceNo?: string
}

export function ReportSubmittedScreen() {
  useDocumentTitle('Report sent')
  const { id } = useParams()
  const state = (useLocation().state ?? {}) as NavState
  const mine = useMyReports()
  const queued = state.kind === 'queued'
  const referenceNo = state.referenceNo ?? mine.data?.find((r) => r.id === id)?.referenceNo

  return (
    <div className="flex flex-col items-center pt-10 text-center">
      {queued ? (
        <CloudOff className="size-16 text-caution" aria-hidden />
      ) : (
        <CheckCircle2 className="size-16 text-ok" aria-hidden />
      )}
      <h1 className="mt-4 font-display text-[28px] font-semibold text-ink">{queued ? 'Saved on this phone' : 'Report sent'}</h1>
      <div className="mt-3">
        {queued ? (
          <StatusChip status="PENDING_SYNC" label="Saved on this device" />
        ) : (
          <StatusChip status="PENDING" label="Waiting for review" />
        )}
      </div>
      {referenceNo && !queued && (
        <p className="tabular mt-4 rounded-full border border-line bg-panel px-4 py-1.5 font-medium text-ink">{referenceNo}</p>
      )}
      <p className="mt-4 max-w-xs text-muted">
        {queued
          ? "It will be sent automatically when the connection returns. You don't need to submit it again."
          : 'A DMC duty officer will check it. You can follow its progress in My reports.'}
      </p>
      <div className="mt-8 w-full space-y-3">
        <Link to={paths.citizen.reports} className="block">
          <Button size="lg" block>
            See my reports
          </Button>
        </Link>
        <Link to={paths.citizen.home} className="block">
          <Button variant="ghost" size="lg" block>
            Back to alerts
          </Button>
        </Link>
      </div>
    </div>
  )
}
