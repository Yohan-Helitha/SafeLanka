import { Copy, Image, Inbox } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { StatusChip } from '@/components/domain'
import { DataTable, EmptyState, ErrorState, Field, Loading, PageHeader, Select } from '@/components/ui'
import { CATEGORY_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useReportQueue } from '@/hooks/reports/useReports'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import type { ReportListItem, ReportStatus } from '@/types'
import { plural, relativeTime } from '@/utils/format'

export function VerificationQueueScreen() {
  useDocumentTitle('Ground reports')
  const navigate = useNavigate()
  const { hazardTypes, districts, hazardTypeName, districtName } = useReferenceData()
  const [status, setStatus] = useState<ReportStatus | 'ALL'>('PENDING')
  const [hazardTypeId, setHazardTypeId] = useState('')
  const [districtId, setDistrictId] = useState('')

  const queue = useReportQueue({
    status,
    hazardTypeId: hazardTypeId || undefined,
    districtId: districtId || undefined,
  })

  return (
    <div>
      <PageHeader
        title="Ground reports"
        subtitle="Check each citizen and volunteer report before it can influence a warning."
      />
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <Field label="Status">
          {(p) => (
            <Select {...p} value={status} onChange={(e) => setStatus(e.target.value as ReportStatus | 'ALL')}>
              <option value="PENDING">Pending</option>
              <option value="NEEDS_MORE_INFO">Needs more info</option>
              <option value="VERIFIED">Verified</option>
              <option value="REJECTED">Rejected</option>
              <option value="ALL">All statuses</option>
            </Select>
          )}
        </Field>
        <Field label="Hazard type">
          {(p) => (
            <Select {...p} value={hazardTypeId} onChange={(e) => setHazardTypeId(e.target.value)}>
              <option value="">All hazard types</option>
              {hazardTypes.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="District">
          {(p) => (
            <Select {...p} value={districtId} onChange={(e) => setDistrictId(e.target.value)}>
              <option value="">All districts</option>
              {districts.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>

      {queue.isLoading && <Loading />}
      {queue.isError && <ErrorState error={queue.error} onRetry={() => void queue.refetch()} />}
      {queue.data && (
        <>
          <p className="tabular mb-2 text-sm text-muted">{plural(queue.data.totalElements, 'report')}</p>
          <DataTable<ReportListItem>
            caption="Ground reports, oldest first"
            rows={queue.data.items}
            rowKey={(r) => r.id}
            onRowClick={(r) => navigate(paths.dmc.report(r.id))}
            empty={<EmptyState icon={Inbox} title="Queue is clear" description="No reports match these filters right now." />}
            columns={[
              { key: 'ref', header: 'Reference', cell: (r) => <span className="tabular font-medium text-signal">{r.referenceNo}</span> },
              {
                key: 'hazard',
                header: 'Hazard · category',
                cell: (r) => `${hazardTypeName(r.hazardTypeId)} · ${CATEGORY_LABEL[r.category] ?? r.category}`,
              },
              { key: 'district', header: 'District', cell: (r) => districtName(r.districtId) },
              { key: 'received', header: 'Received', cell: (r) => <span className="text-muted">{relativeTime(r.syncedAt)}</span> },
              {
                key: 'evidence',
                header: 'Evidence',
                cell: (r) => (
                  <span className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                    {r.hasPhoto && (
                      <span className="inline-flex items-center gap-1 text-muted">
                        <Image className="size-4" aria-hidden /> Photo
                      </span>
                    )}
                    {r.hasDuplicates && (
                      <span className="inline-flex items-center gap-1 text-caution">
                        <Copy className="size-4" aria-hidden /> Possible duplicate
                      </span>
                    )}
                    {!r.hasPhoto && !r.hasDuplicates && <span className="text-faint">—</span>}
                  </span>
                ),
              },
              { key: 'status', header: 'Status', cell: (r) => <StatusChip status={r.status} /> },
            ]}
          />
        </>
      )}
    </div>
  )
}
