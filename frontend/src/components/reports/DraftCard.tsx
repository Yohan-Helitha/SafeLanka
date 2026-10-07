import { FilePen } from 'lucide-react'
import { Link } from 'react-router-dom'
import { StatusChip } from '@/components/domain'
import { Button } from '@/components/ui'
import { CATEGORY_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useToast } from '@/context/ToastContext'
import { draftToInput, removeDraft } from '@/hooks/reports/useReportDrafts'
import { useSubmitReport } from '@/hooks/reports/useReports'
import { useReferenceData } from '@/hooks/shared'
import { ApiErrorNotice } from '@/components/domain'
import type { ReportDraft } from '@/services/offline/drafts'
import { relativeTime } from '@/utils/format'

/** A report that was filled in but not sent yet: continue editing, send it, or delete it. */
export function DraftCard({ draft }: { draft: ReportDraft }) {
  const { hazardTypeName } = useReferenceData()
  const { toast } = useToast()
  const submit = useSubmitReport()
  const input = draftToInput(draft)
  const title = draft.hazardTypeId
    ? `${hazardTypeName(draft.hazardTypeId)}${draft.category ? ` · ${CATEGORY_LABEL[draft.category] ?? draft.category}` : ''}`
    : 'Report not finished'

  const send = () => {
    if (!input) return
    submit.mutate(input, {
      onSuccess: (result) => {
        void removeDraft(draft.id)
        toast(result.queued ? 'Saved. It will be sent when you are back online.' : 'Report sent')
      },
    })
  }

  return (
    <article className="rounded-card border border-signal/50 bg-panel p-4">
      <header className="flex items-start justify-between gap-3">
        <div>
          <h3 className="flex items-center gap-1.5 font-semibold text-ink">
            <FilePen className="size-4 text-signal" aria-hidden />
            {title}
          </h3>
          <p className="text-sm text-muted">Saved {relativeTime(draft.updatedAt)} · not sent yet</p>
        </div>
        <StatusChip status="PENDING_SYNC" label={input ? 'Ready to send' : 'Unfinished'} />
      </header>
      {draft.description.trim() && <p className="mt-2 line-clamp-2 text-[15px] text-ink">{draft.description.trim()}</p>}
      {!input && <p className="mt-2 text-sm text-muted">Finish the missing details, then send it.</p>}
      <div className="mt-3">
        <ApiErrorNotice error={submit.error} />
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        <Link to={`${paths.citizen.report}?draft=${draft.id}`}>
          <Button variant="secondary" size="sm">
            {input ? 'Review' : 'Continue'}
          </Button>
        </Link>
        <Button size="sm" disabled={!input} loading={submit.isPending} onClick={send}>
          Send
        </Button>
        <Button variant="ghost" size="sm" disabled={submit.isPending} onClick={() => void removeDraft(draft.id)}>
          Delete
        </Button>
      </div>
    </article>
  )
}
