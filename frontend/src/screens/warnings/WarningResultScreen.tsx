import { useParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useState } from 'react'
import { SeverityBadge, StatusChip } from '@/components/domain'
import { Button, Card, DataTable, ErrorState, Field, Loading, PageHeader, Select, Stat } from '@/components/ui'
import { WarningActions } from '@/components/warnings/WarningActions'
import { CHANNEL_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useDeliveries, useWarning } from '@/hooks/warnings/useWarnings'
import { CHART } from '@/theme/tokens'
import type { Channel, Delivery, DeliveryFilter } from '@/types'
import { formatDateTime, formatNumber, formatPercent, formatTime } from '@/utils/format'

const PAGE = 25

export function WarningResultScreen() {
  const { id = '' } = useParams()
  const { districtName } = useReferenceData()
  const warning = useWarning(id)
  const [status, setStatus] = useState<NonNullable<DeliveryFilter['status']>>('ALL')
  const [channel, setChannel] = useState<NonNullable<DeliveryFilter['channel']>>('ALL')
  const [page, setPage] = useState(0)
  const deliveries = useDeliveries(id, { status, channel, page, size: PAGE })
  useDocumentTitle(warning.data?.title ?? 'Warning')

  if (warning.isLoading) return <Loading />
  if (warning.isError || !warning.data) return <ErrorState error={warning.error} onRetry={() => void warning.refetch()} />
  const w = warning.data
  const s = deliveries.data?.summary ?? w.deliverySummary
  const total = s.delivered + s.failed
  const chart = (Object.keys(s.byChannel) as Channel[])
    .filter((c) => s.byChannel[c].delivered + s.byChannel[c].failed > 0)
    .map((c) => ({ channel: CHANNEL_LABEL[c], Delivered: s.byChannel[c].delivered, Failed: s.byChannel[c].failed }))
  const pageData = deliveries.data?.items

  return (
    <div>
      <PageHeader
        backTo={paths.dmc.warnings}
        backLabel="Warnings"
        title={w.title}
        subtitle={`Issued ${formatDateTime(w.issuedAt)} · ${w.districtIds.map(districtName).join(', ')}`}
        actions={
          <>
            <SeverityBadge level={w.level} size="lg" />
            <StatusChip status={w.status} />
          </>
        }
      />
      {w.cancelReason && <p className="mb-3 rounded-control border border-line bg-panel p-3 text-sm text-muted">Cancelled: {w.cancelReason}</p>}

      <WarningActions warning={w} />

      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="People targeted" value={formatNumber(s.targeted)} tone="signal" />
        <Stat label="Messages delivered" value={formatNumber(s.delivered)} />
        <Stat label="Failed" value={formatNumber(s.failed)} tone={s.failed > 0 ? 'danger' : 'default'} />
        <Stat label="Delivery rate" value={total ? formatPercent(s.delivered / total) : '—'} />
      </div>

      <div className="mb-4 grid gap-4 lg:grid-cols-2">
        <Card title="By channel" description="A failing channel never stops the others.">
          <div role="img" aria-label="Delivered and failed messages by channel">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={chart} margin={{ top: 8, right: 8, bottom: 0, left: -12 }}>
                <CartesianGrid stroke={CHART.grid} strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="channel" stroke={CHART.axis} tick={{ fontSize: 12 }} />
                <YAxis stroke={CHART.axis} tick={{ fontSize: 12 }} allowDecimals={false} />
                <Tooltip contentStyle={{ background: '#0B1324', border: '1px solid #26324A', borderRadius: 8, color: '#E2E8F0' }} cursor={{ fill: 'rgba(148,163,184,0.08)' }} />
                <Legend />
                <Bar dataKey="Delivered" stackId="a" fill={CHART.signal} isAnimationActive={false} />
                <Bar dataKey="Failed" stackId="a" fill={CHART.danger} isAnimationActive={false} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card title="Message">
          <p className="text-[15px] text-ink">{w.message}</p>
          <h3 className="mt-3 text-sm font-medium text-muted">What people should do</h3>
          <p className="text-[15px] text-ink">{w.instructions}</p>
          <h3 className="mt-3 text-sm font-medium text-muted">SMS</h3>
          <p className="mt-1 rounded-control bg-raised p-2.5 text-sm text-ink">{w.smsText}</p>
        </Card>
      </div>

      <Card title="Deliveries" padded>
        <div className="mb-3 grid max-w-md grid-cols-2 gap-3">
          <Field label="Result">
            {(p) => (
              <Select {...p} value={status} onChange={(e) => { setStatus(e.target.value as typeof status); setPage(0) }}>
                <option value="ALL">All</option>
                <option value="DELIVERED">Delivered</option>
                <option value="FAILED">Failed</option>
              </Select>
            )}
          </Field>
          <Field label="Channel">
            {(p) => (
              <Select {...p} value={channel} onChange={(e) => { setChannel(e.target.value as typeof channel); setPage(0) }}>
                <option value="ALL">All channels</option>
                <option value="PUSH">App notification</option>
                <option value="SMS">SMS</option>
                <option value="AUDIBLE">Audible alarm</option>
              </Select>
            )}
          </Field>
        </div>
        {deliveries.isLoading && <Loading />}
        {pageData && (
          <>
            <DataTable<Delivery>
              caption="Message deliveries"
              rows={pageData.items}
              rowKey={(d) => d.id}
              empty={<p className="py-6 text-center text-muted">No deliveries match these filters.</p>}
              columns={[
                { key: 'district', header: 'District', cell: (d) => districtName(d.districtId) },
                { key: 'channel', header: 'Channel', cell: (d) => CHANNEL_LABEL[d.channel] },
                { key: 'result', header: 'Result', cell: (d) => <StatusChip status={d.status} /> },
                { key: 'time', header: 'Time', cell: (d) => <span className="tabular text-muted">{formatTime(d.attemptedAt)}</span> },
                { key: 'reason', header: 'Failure reason', cell: (d) => d.failureReason ?? <span className="text-faint">—</span> },
              ]}
            />
            <div className="mt-3 flex items-center justify-between text-sm text-muted">
              <span className="tabular">
                Showing {pageData.items.length} of {pageData.totalElements}
              </span>
              <span className="flex gap-2">
                <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  Previous
                </Button>
                <Button size="sm" variant="secondary" disabled={page + 1 >= pageData.totalPages} onClick={() => setPage((p) => p + 1)}>
                  Next
                </Button>
              </span>
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
