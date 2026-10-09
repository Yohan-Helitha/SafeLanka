import { ExternalLink, ImageOff } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiErrorNotice, StatusChip } from '@/components/domain'
import { RejectDialog } from '@/components/reports/RejectDialog'
import { Button, Card, CharCount, Dialog, ErrorState, Field, LocationMap, Loading, PageHeader, TextArea } from '@/components/ui'
import { LIMITS } from '@/constants/app'
import { CATEGORY_LABEL, REJECTION_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useToast } from '@/context/ToastContext'
import { useReport, useRejectReport, useRequestInfo, useVerifyReport } from '@/hooks/reports/useReports'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { formatCoords, formatDateTime, relativeTime } from '@/utils/format'
import { mapLink } from '@/utils/geo'

export function ReportDetailScreen() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { hazardTypeName, districtName } = useReferenceData()
  const report = useReport(id)
  const verify = useVerifyReport()
  const reject = useRejectReport()
  const requestInfo = useRequestInfo()

  const [rejecting, setRejecting] = useState(false)
  const [asking, setAsking] = useState(false)
  const [comment, setComment] = useState('')
  const [severity, setSeverity] = useState(2)
  useDocumentTitle(report.data?.referenceNo ?? 'Report')

  if (report.isLoading) return <Loading />
  if (report.isError || !report.data) return <ErrorState error={report.error} onRetry={() => void report.refetch()} />
  const r = report.data

  const open = r.status === 'PENDING' || r.status === 'NEEDS_MORE_INFO'
  const backToQueue = () => navigate(paths.dmc.reports)
  const lateSync = new Date(r.syncedAt).getTime() - new Date(r.capturedAt).getTime() > 30 * 60_000
  const commentTooShort = comment.trim().length < LIMITS.comment.min

  return (
    <div>
      <PageHeader
        backTo={paths.dmc.reports}
        backLabel="Ground reports"
        title={<span className="tabular">{r.referenceNo}</span>}
        subtitle={`${hazardTypeName(r.hazardTypeId)} · ${CATEGORY_LABEL[r.category] ?? r.category} · ${districtName(r.districtId)}`}
        actions={<StatusChip status={r.status} />}
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_380px]">
        <div className="space-y-4">
          <Card title="Evidence">
            {r.photoUrl ? (
              <img src={r.photoUrl} alt={`Photo attached to ${r.referenceNo}`} className="max-h-80 w-full rounded-control bg-black/40 object-contain" />
            ) : (
              <p className="flex items-center gap-2 text-muted">
                <ImageOff className="size-5" aria-hidden /> No photo was attached
              </p>
            )}
            <h3 className="mt-4 text-sm font-medium text-muted">Reporter's description</h3>
            <p className="mt-1 text-ink">{r.description}</p>
          </Card>
          <Card title="Location">
            {r.latitude !== undefined && r.longitude !== undefined ? (
              <div className="space-y-3">
              <LocationMap label={`Map showing where ${r.referenceNo} was reported`} markers={[{ id: r.id, latitude: r.latitude, longitude: r.longitude, label: r.referenceNo }]} />
              <p className="flex flex-wrap items-center gap-3">
                <span className="tabular text-ink">{formatCoords(r.latitude, r.longitude)}</span>
                <a
                  className="inline-flex items-center gap-1 text-signal hover:underline"
                  href={mapLink({ latitude: r.latitude, longitude: r.longitude })}
                  target="_blank"
                  rel="noreferrer"
                >
                  Open map <ExternalLink className="size-3.5" aria-hidden />
                </a>
              </p>
              </div>
            ) : (
              <p className="rounded-control border border-caution/50 bg-caution/10 p-3 text-sm text-ink">
                Described by the reporter (no GPS): {r.manualLocationText}
              </p>
            )}
          </Card>
        </div>

        <div className="space-y-4">
          <Card title="Details">
            <dl className="space-y-2.5 text-sm">
              <Item label="Reported by" value={`${r.reporter.fullName} (${r.reporter.role.toLowerCase().replace(/_/g, ' ')})`} />
              <Item label="Seen at" value={formatDateTime(r.capturedAt)} />
              <Item label="Received" value={`${formatDateTime(r.syncedAt)}${lateSync ? ' (sent after reconnecting)' : ''}`} />
              {r.reviewedAt && <Item label="Reviewed" value={`${formatDateTime(r.reviewedAt)} by ${r.reviewedBy ?? '—'}`} />}
              {r.rejectionReason && <Item label="Reason" value={REJECTION_LABEL[r.rejectionReason]} />}
              {r.reviewComment && <Item label="Comment" value={r.reviewComment} />}
              {r.reporterReply && <Item label="Reporter's answer" value={r.reporterReply} />}
            </dl>
          </Card>

          {r.possibleDuplicates.length > 0 && (
            <Card title="Possible duplicates" description="Same hazard type within 500 m and 2 hours.">
              <ul className="space-y-1.5 text-sm">
                {r.possibleDuplicates.map((d) => (
                  <li key={d.id} className="flex justify-between gap-3">
                    <Link className="tabular text-signal hover:underline" to={paths.dmc.report(d.id)}>
                      {d.referenceNo}
                    </Link>
                    <span className="tabular text-muted">{d.distanceMetres} m away</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}

          {open && (
            <Card title="Decision">
              <div className="space-y-3">
                <ApiErrorNotice error={verify.error} />
                <div role="group" aria-label="Hazard severity">
                  <p className="mb-1.5 text-sm text-muted">How dangerous is this? (1 low, 5 very dangerous)</p>
                  <div className="flex gap-1.5">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button
                        key={n}
                        type="button"
                        aria-pressed={n === severity}
                        onClick={() => setSeverity(n)}
                        className={`h-9 flex-1 rounded-control border text-sm font-medium ${
                          n === severity ? 'border-signal bg-signal/15 text-signal' : 'border-line text-muted hover:border-signal/60 hover:text-ink'
                        }`}
                      >
                        {n}
                      </button>
                    ))}
                  </div>
                </div>
                <Button
                  size="lg"
                  block
                  loading={verify.isPending}
                  onClick={() =>
                    verify.mutate({ id: r.id, severity }, {
                      onSuccess: () => {
                        toast(`${r.referenceNo} verified`)
                        backToQueue()
                      },
                    })
                  }
                >
                  Verify report
                </Button>
                {r.status === 'PENDING' && (
                  <Button variant="secondary" block onClick={() => setAsking(true)}>
                    Ask for more information
                  </Button>
                )}
                <Button variant="ghost" block onClick={() => setRejecting(true)}>
                  Reject…
                </Button>
              </div>
            </Card>
          )}
          {!open && <p className="text-sm text-faint">Reviewed {relativeTime(r.reviewedAt)}. No further decision needed.</p>}
        </div>
      </div>

      <RejectDialog
        open={rejecting}
        onClose={() => setRejecting(false)}
        loading={reject.isPending}
        error={reject.error}
        onConfirm={(reason, text) =>
          reject.mutate(
            { id: r.id, reason, comment: text },
            {
              onSuccess: () => {
                toast(`${r.referenceNo} rejected`)
                backToQueue()
              },
            },
          )
        }
      />

      <Dialog
        open={asking}
        onClose={() => setAsking(false)}
        title="Ask for more information"
        description="The reporter sees your question in My reports."
        footer={
          <>
            <Button variant="ghost" onClick={() => setAsking(false)}>
              Cancel
            </Button>
            <Button
              loading={requestInfo.isPending}
              disabled={commentTooShort}
              onClick={() =>
                requestInfo.mutate(
                  { id: r.id, comment: comment.trim() },
                  {
                    onSuccess: () => {
                      toast('Question sent')
                      setAsking(false)
                      setComment('')
                    },
                  },
                )
              }
            >
              Send question
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <Field label="Your question" required aside={<CharCount value={comment} max={LIMITS.comment.max} />}>
            {(p) => <TextArea {...p} rows={3} maxLength={LIMITS.comment.max} value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Can you add the street name and a photo?" />}
          </Field>
          <ApiErrorNotice error={requestInfo.error} />
        </div>
      </Dialog>
    </div>
  )
}

function Item({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-faint">{label}</dt>
      <dd className="text-ink">{value}</dd>
    </div>
  )
}
